import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import ui.MainWindow
import ui.viewmodel.MainViewModel

fun main() = application {
    val viewModel = MainViewModel()
    
    Window(
        onCloseRequest = {
            viewModel.dispose()
            exitApplication()
        },
        title = "LogcatFilter",
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1200.dp, 800.dp)
        )
    ) {
        MainWindow(viewModel = viewModel)
    }
}
