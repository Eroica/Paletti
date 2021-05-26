import app.paletti.lib.Leptonica
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import javafx.beans.InvalidationListener
import javafx.beans.property.*
import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.paint.Color
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

interface IPosterizedImage {
    val path: String
    val image: Image
    val colors: List<Color>
}

interface IViewModel {
    val count: IntegerProperty
    val isBlackWhite: BooleanProperty
    val notification: StringProperty
    val image: Observable<IPosterizedImage>

    fun load(path: String)
    fun load(image: Image)
    fun save(destination: File)
}

data class PosterizedPix(
    private val sqlImage: SqlImage,
    private val cacheDir: File,
    private val databasePath: String
) : IPosterizedImage by sqlImage {
    override val path: String
        get() {
            return cacheDir.resolve("${sqlImage.id}.png").toString()
        }

    override val image: Image
        get() {
            return Image(File(path).toURI().toString())
        }

    init {
        val ok = Leptonica.posterize2(sqlImage.id, databasePath)
        if (ok != Leptonica.OK) {
            throw LeptonicaError
        }
    }
}

class ViewModel(
    private val images: SqlImages,
    private val databasePath: String,
    private val cacheDir: File
) : IViewModel {
    override val count: IntegerProperty = SimpleIntegerProperty(6)
    override val isBlackWhite: BooleanProperty = SimpleBooleanProperty(false)
    override val notification: StringProperty = SimpleStringProperty()

    private val _image = BehaviorSubject.create<IPosterizedImage>()
    override val image: Observable<IPosterizedImage>
        get() = _image.subscribeOn(Schedulers.io()).observeOn(JavaFxScheduler.platform())

    private val _posterize = PublishSubject.create<Unit>()
    private val disposables = CompositeDisposable()
    private var hasImage = false

    private val onChangeListener = InvalidationListener {
        if (hasImage) {
            _posterize.onNext(Unit)
        }
    }

    init {
        disposables.add(_posterize.debounce(100, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(JavaFxScheduler.platform())
            .subscribe {
                try {
                    images[1].setParameters(count.get(), isBlackWhite.get()) // TODO Remove duplication
                    _image.onNext(PosterizedPix(images[1], cacheDir, databasePath))
                } catch (e: LeptonicaError) {
                    notification.value = e.message
                } catch (e: Uninitialized) {
                    notification.value = e.message
                }
            }
        )
        this.count.addListener(onChangeListener)
        this.isBlackWhite.addListener(onChangeListener)
    }

    override fun load(path: String) {
        hasImage = true
        images.delete(1)
        val imageId = images.add(count.get(), isBlackWhite.get(), path)
        _posterize.onNext(Unit)
    }

    override fun load(image: Image) {
        disposables.add(Completable.fromAction {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", cacheDir.resolve("tmp.png"))
        }
            .subscribeOn(Schedulers.io())
            .observeOn(JavaFxScheduler.platform())
            .subscribe { load(cacheDir.resolve("tmp.png").toString()) })
    }

    override fun save(destination: File) {
        disposables.add(Completable.fromAction {
            File(images[1].path).copyTo(destination, true)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(JavaFxScheduler.platform())
            .subscribe {
                notification.value = "Saved image to ${Paths.get(destination.toURI())}"
            })
    }

    fun onDestroy() {
        disposables.dispose()
    }
}
