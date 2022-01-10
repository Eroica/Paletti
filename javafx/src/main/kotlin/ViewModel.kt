import app.paletti.lib.Leptonica
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observables.ConnectableObservable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.beans.InvalidationListener
import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
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
    val isCropImage: BooleanProperty
    val isRestoreImage: BooleanProperty
    val notification: Observable<String>
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
    private val id: Int,
    private val images: SqlImages,
    private val databasePath: String,
    private val cacheDir: File
) : IViewModel {
    override val count: IntegerProperty = SimpleIntegerProperty(6)
    override val isBlackWhite: BooleanProperty = SimpleBooleanProperty(false)
    override val isCropImage: BooleanProperty = SimpleBooleanProperty(true)
    override val isRestoreImage: BooleanProperty = SimpleBooleanProperty(false)
    override val notification: BehaviorSubject<String> = BehaviorSubject.create()

    private var imagePath: String? = null

    private val _posterize = PublishSubject.create<String>()
    override val image = _posterize.debounce(100, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.computation())
        .map {
            images.delete(id)
            images.add(id, count.get(), isBlackWhite.get(), it)
            images[id].setParameters(count.get(), isBlackWhite.get())
            if (Leptonica.posterize2(id, databasePath) != Leptonica.OK) throw LeptonicaError
            PosterizedPix(images[id], cacheDir)
        }
        .observeOn(JavaFxScheduler.platform())
        .doOnError { it.message?.let { message -> notification.onNext(message) } }
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
            cacheDir.resolve("${images[id].id}.png").copyTo(destination, true)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(JavaFxScheduler.platform())
            .subscribe {
                notification.onNext("Saved image to ${Paths.get(destination.toURI())}")
            })
    }

    fun onDestroy() {
        if (!isRestoreImage.get()) {
            images.delete(id)
        }
        count.removeListener(onChangeListener)
        isBlackWhite.removeListener(onChangeListener)
        disposables.dispose()
    }
}
