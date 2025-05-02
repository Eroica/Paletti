import javafx.scene.paint.Color
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.sql.PreparedStatement

interface IPosterizedImage {
    val source: String
    val colors: List<Color>
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

class PosterizedPix(
    private val sqlImage: SqlImage,
    cacheDir: File
) : IPosterizedImage by sqlImage {
    val path: String = cacheDir.resolve("${sqlImage.id}.png").toURI().toString()
}
