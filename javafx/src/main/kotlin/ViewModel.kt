import app.paletti.lib.Leptonica
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.schedulers.Schedulers
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
    val source: String
    val colors: List<Color>
}

interface IViewModel {
    val count: IntegerProperty
    val isBlackWhite: BooleanProperty
    val notification: StringProperty
    val image: ConnectableObservable<PosterizedPix>

    fun load(path: String)
    fun load(image: Image)
    fun save(destination: File)
}

data class PosterizedPix(
    private val sqlImage: SqlImage,
    private val cacheDir: File,
) : IPosterizedImage by sqlImage {
    val path: String = cacheDir.resolve("${sqlImage.id}.png").toURI().toString()
}

class ViewModel(
    private val images: SqlImages,
    private val databasePath: String,
    private val cacheDir: File
) : IViewModel {
    override val count: IntegerProperty = SimpleIntegerProperty(6)
    override val isBlackWhite: BooleanProperty = SimpleBooleanProperty(false)
    override val notification: StringProperty = SimpleStringProperty()

    private var imagePath: String? = null

    private val _posterize = PublishSubject.create<String>()
    override val image = _posterize.debounce(100, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.computation())
        .map {
            images.delete(1)
            images.add(count.get(), isBlackWhite.get(), it)
            images[1].setParameters(count.get(), isBlackWhite.get())
            Leptonica.posterize2(1, databasePath)
            PosterizedPix(images[1], cacheDir)
        }
        .publish()
    private val disposables = CompositeDisposable()

    private val onChangeListener = InvalidationListener {
        imagePath?.let {
            _posterize.onNext(it)
        }
    }

    init {
        this.disposables.add(this.image.connect())
        this.count.addListener(this.onChangeListener)
        this.isBlackWhite.addListener(this.onChangeListener)
    }

    override fun load(path: String) {
        imagePath = path
        _posterize.onNext(path)
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
            cacheDir.resolve("${images[1].id}.png").copyTo(destination, true)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(JavaFxScheduler.platform())
            .subscribe {
                notification.value = "Saved image to ${Paths.get(destination.toURI())}"
            })
    }

    fun onDestroy() {
        count.removeListener(onChangeListener)
        isBlackWhite.removeListener(onChangeListener)
        disposables.dispose()
    }
}
