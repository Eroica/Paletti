import app.paletti.lib.Windows
import java.io.Closeable
import java.io.File
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
