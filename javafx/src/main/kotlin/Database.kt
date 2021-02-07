import javafx.scene.image.Image
import javafx.scene.paint.Color
import java.io.Closeable
import java.io.File
import java.sql.DriverManager
import java.sql.PreparedStatement

class Database(path: File, cacheDir: File) : Closeable {
    private val db = DriverManager.getConnection("jdbc:sqlite:${path.absolutePath}")

    init {
        db.createStatement().apply {
            addBatch(
                """CREATE TABLE IF NOT EXISTS image (
                id INTEGER PRIMARY KEY,
                count INTEGER NON NULL DEFAULT 3,
                is_black_white INTEGER NON NULL DEFAULT 0,
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
            VALUES ("cache", "$cacheDir")"""
            )
            executeBatch()
        }
    }

    override fun close() {
        db.close()
    }

    fun statement(sql: String): PreparedStatement {
        return db.prepareStatement(sql)
    }
}

data class SqlImage(
    val id: Int,
    private val getPath: PreparedStatement,
    private val getColors: PreparedStatement,
    private val setParameters: PreparedStatement
) : IPosterizedImage {
    override val path: String
        get() = getPath.apply { setInt(1, id) }.executeQuery().use {
            it.getString(1)
        }
    override val image: Image
        get() = Image(File(path).toURI().toString())
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

    fun setParameters(count: Int, isBlackWhite: Boolean) {
        setParameters.apply {
            setInt(1, count)
            setBoolean(2, isBlackWhite)
            setInt(3, id)
        }.executeUpdate()
    }
}

class SqlImages(database: Database) {
    private val GET_PATH = database.statement("""UPDATE image SET count=?, is_black_white=? WHERE id=?""")
    private val GET_COLORS = database.statement("""SELECT rgb FROM color WHERE image_id=?""")
    private val GET_SOURCE = database.statement("""SELECT source FROM image WHERE id=?""")
    private val ADD_IMAGE = database.statement("""INSERT INTO image (count, is_black_white, source) VALUES (?, ?, ?)""")
    private val DELETE_IMAGES = database.statement("""DELETE FROM image""")

    operator fun get(imageId: Int): SqlImage {
        return SqlImage(imageId, GET_SOURCE, GET_COLORS, GET_PATH)
    }

    fun add(count: Int, isBlackWhite: Boolean, source: String): Int {
        ADD_IMAGE.apply {
            setInt(1, count)
            setInt(2, if (isBlackWhite) 1 else 0)
            setString(3, source)
        }.execute()
        return ADD_IMAGE.generatedKeys.getInt(1)
    }

    fun delete(imageId: Int) {
        DELETE_IMAGES.execute()
    }
}
