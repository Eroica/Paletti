import app.paletti.lib.Windows
import javafx.scene.paint.Color
import net.harawata.appdirs.AppDirsFactory
import java.io.Closeable
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

private const val DB_INITIAL_VERSION = 0
private const val DB_CURRENT_VERSION = 2

class Database(path: File, cacheDir: File) : Closeable {
    private val db = DriverManager.getConnection("jdbc:sqlite:${path.absolutePath}")

    init {
        /* I didn't introduce database versions before, so the first migration is applied iff the
         * database file already exists.  */
        val isFirstRun = db.createStatement().executeQuery("""SELECT name FROM sqlite_master WHERE type='table' AND name='image';""")
            .use { it.getString(1) == null }

        if (isFirstRun) {
            initialize(cacheDir)
        } else if (getVersion() < 2) {
            migrateTo2()
        }

        /* After this, the current database version is 2 */
    }

    override fun close() {
        db.close()
    }

    fun statement(sql: String): PreparedStatement {
        return db.prepareStatement(sql)
    }

    var isRestoreImage: Boolean
        get() = db.createStatement()
            .executeQuery("""SELECT `value` FROM environment WHERE name="is_restore_image";""")
            .use { it.getBoolean(1) }
        set(value) {
            db.prepareStatement("""UPDATE environment SET `value`=? WHERE name="is_restore_image"""").apply {
                setBoolean(1, value)
            }.executeUpdate()
        }

    var isAlwaysDarkMode: Boolean
        get() = db.createStatement()
            .executeQuery("""SELECT `value` FROM environment WHERE name="is_always_dark_mode";""")
            .use { it.getBoolean(1) }
        set(value) {
            db.prepareStatement("""UPDATE environment SET `value`=? WHERE name="is_always_dark_mode"""").apply {
                setBoolean(1, value)
            }.executeUpdate()
        }

    fun monotonicId(): Int {
        return db.createStatement().executeQuery("""SELECT max(id) FROM image;""").use {
            it.getInt(1)
        }
    }

    fun isPrefersDarkMode() = Windows.isdarkmode() || isAlwaysDarkMode

    private fun initialize(cacheDir: File) {
        db.createStatement().apply {
            addBatch(
                """CREATE TABLE IF NOT EXISTS image (
                    id INTEGER PRIMARY KEY,
                    count INTEGER NON NULL DEFAULT 3,
                    is_black_white INTEGER NON NULL DEFAULT 0,
                    is_dither INTEGER NON NULL DEFAULT 0,
                    source VARCHAR NON NULL
                );"""
            )
            addBatch(
                """CREATE TABLE IF NOT EXISTS color (
                    id INTEGER PRIMARY KEY,
                    rgb INTEGER NOT NULL,
                    image_id INTEGER NOT NULL,
                    FOREIGN KEY (image_id) REFERENCES image (id)
                );"""
            )
            addBatch(
                """CREATE TABLE IF NOT EXISTS environment (
                    name VARCHAR PRIMARY KEY,
                    value VARCHAR NOT NULL,
                    UNIQUE (name)
                );"""
            )
            addBatch(
                """INSERT OR IGNORE INTO environment (name, value)
                VALUES ("cache", "$cacheDir");"""
            )
            addBatch(
                """INSERT OR IGNORE INTO environment (name, value)
                VALUES ("is_restore_image", 0);"""
            )
            addBatch(
                """INSERT OR IGNORE INTO environment (name, value)
                VALUES ("is_always_dark_mode", 0);"""
            )
            executeBatch()
        }
        setVersion(DB_CURRENT_VERSION)
    }

    private fun migrateTo2() {
        db.createStatement().execute("""ALTER TABLE image ADD is_dither INTEGER NON NULL DEFAULT 0;""")
        setVersion(DB_CURRENT_VERSION)
    }

    private fun getVersion(): Int {
        return db.createStatement().use {
            it.executeQuery("""PRAGMA user_version""").use {
                it.getInt(1)
            }
        }
    }

    private fun setVersion(version: Int) {
        db.createStatement().use {
            it.executeUpdate(String.format("PRAGMA user_version = %d;", version))
        }
    }
}

class AppDir : File(AppDirsFactory.getInstance().getUserCacheDir(APP_NAME, null, null)) {
    val databasePath = resolve(DB_NAME).toString()
    val database by lazy { Database(resolve(DB_NAME), this) }

    init {
        mkdirs()
    }
}

data class SqlImage(
    val id: Int,
    private val getSource: PreparedStatement,
    private val getColors: PreparedStatement,
    private val setParameters: PreparedStatement,
    private val getCount: PreparedStatement,
    private val getIsBlackWhite: PreparedStatement,
    private val getIsDither: PreparedStatement
) : IPosterizedImage {
    override val colors: List<Color>
        get() {
            return ArrayList<Color>().apply {
                getColors.apply { setInt(1, id) }.executeQuery().use {
                    while (it.next()) {
                        val rgb = it.getInt(1)
                        add(Color.rgb((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF))
                    }
                }
            }
        }

    override val source: String
        get() = getSource.apply { setInt(1, id) }.executeQuery().use {
            it.getString(1)
        }

    val count: Int
        get() = getCount.apply { setInt(1, id) }.executeQuery().use {
            it.getInt(1)
        }

    val isBlackWhite: Boolean
        get() = getIsBlackWhite.apply { setInt(1, id) }.executeQuery().use {
            it.getBoolean(1)
        }

    val isDither: Boolean
        get() = getIsDither.apply { setInt(1, id) }.executeQuery().use {
            it.getBoolean(1)
        }

    fun setParameters(count: Int, isBlackWhite: Boolean, isDither: Boolean) {
        setParameters.apply {
            setInt(1, count)
            setBoolean(2, isBlackWhite)
            setBoolean(3, isDither)
            setInt(4, id)
        }.executeUpdate()
    }
}

class SqlImages(database: Database) {
    private val SET_PARAMETERS = database.statement("""UPDATE image SET count=?, is_black_white=?, is_dither=? WHERE id=?""")
    private val GET_COLORS = database.statement("""SELECT rgb FROM color WHERE image_id=?""")
    private val GET_SOURCE = database.statement("""SELECT source FROM image WHERE id=?""")
    private val ADD_IMAGE = database.statement("""INSERT INTO image (id, count, is_black_white, is_dither, source) VALUES (?, ?, ?, ?, ?) RETURNING id""")
    private val DELETE_IMAGE = database.statement("""DELETE FROM image WHERE id=?""")
    private val GET_COUNT = database.statement("""SELECT count FROM image WHERE id=?""")
    private val GET_IS_BLACK_WHITE = database.statement("""SELECT is_black_white FROM image WHERE id=?""")
    private val GET_IS_DITHER = database.statement("""SELECT is_dither FROM image WHERE id=?""")

    operator fun get(imageId: Int): SqlImage {
        return SqlImage(imageId, GET_SOURCE, GET_COLORS, SET_PARAMETERS, GET_COUNT, GET_IS_BLACK_WHITE, GET_IS_DITHER)
    }

    fun add(id: Int, count: Int, isBlackWhite: Boolean, isDither: Boolean, source: String): Int {
        ADD_IMAGE.apply {
            setInt(1, id)
            setInt(2, count)
            setInt(3, if (isBlackWhite) 1 else 0)
            setInt(4, if (isDither) 1 else 0)
            setString(5, source)
        }.executeQuery().use {
            return it.getInt(1)
        }
    }

    fun delete(imageId: Int) {
        DELETE_IMAGE.setInt(1, imageId)
        DELETE_IMAGE.execute()
    }
}
