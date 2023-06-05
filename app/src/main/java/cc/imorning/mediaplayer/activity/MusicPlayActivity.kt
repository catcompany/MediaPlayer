package cc.imorning.mediaplayer.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import cc.imorning.mediaplayer.IMusicPlayerService
import cc.imorning.mediaplayer.R
import cc.imorning.mediaplayer.activity.ui.theme.MediaTheme
import cc.imorning.mediaplayer.data.MusicItem
import cc.imorning.mediaplayer.service.MusicPlayService
import cc.imorning.mediaplayer.viewmodel.MusicPlayViewModel
import cc.imorning.mediaplayer.viewmodel.MusicPlayViewModelFactory
import coil.compose.AsyncImage
import com.google.common.util.concurrent.MoreExecutors

private const val TAG = "MusicPlayActivity"

class MusicPlayActivity : BaseActivity() {

    private lateinit var viewModel: MusicPlayViewModel
    private var musicItem: MusicItem? = null

    private var isBound = false
    private var musicPlayService: IMusicPlayerService? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicPlayService = IMusicPlayerService.Stub.asInterface(service)
            viewModel.init(this@MusicPlayActivity, musicPlayService)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        musicItem = intent.getParcelableExtra(ITEM)
        viewModel = ViewModelProvider(this, MusicPlayViewModelFactory())[MusicPlayViewModel::class.java]

        val service = Intent(this, MusicPlayService::class.java)
        service.putExtra(MusicPlayService.MUSIC_ID, musicItem!!.id)
        ContextCompat.startForegroundService(this, service)
        bindService(service, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MediaTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayScreen(viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicPlayService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
                { controllerFuture.get() },
                MoreExecutors.directExecutor()
        )
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        super.onDestroy()
    }

    companion object {
        const val ITEM: String = "item"
    }

}

@Composable
fun MusicPlayScreen(viewModel: MusicPlayViewModel, modifier: Modifier = Modifier) {

    val musicInfo = viewModel.musicItem.collectAsState()

    Column(modifier = modifier) {
        Text(
                text = musicInfo.value.name.orEmpty(),
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                textAlign = TextAlign.Center
        )
        Text(
                text = musicInfo.value.artists.orEmpty(),
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                textAlign = TextAlign.Center
        )
        AsyncImage(
                model = "https://p2.music.126.net/ryk8Gu64rOhlYn0pc2Q8Ww==/109951168090271827.jpg",
                contentDescription = "歌曲封面",
                modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 0.5f, fill = true),
        )
        ProgressContent(viewModel)
        PlayerControlView(viewModel)
    }

}

@Composable
fun ProgressContent(viewModel: MusicPlayViewModel) {


    val position = viewModel.currentProgress.collectAsState()
    val maxTime = viewModel.maxSecond.collectAsState()
    val currentTime = viewModel.currentSeconds.collectAsState()

    Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp)
    ) {
        Text(
                text = currentTime.value,
                modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 8.dp)
        )
        Slider(
                modifier = Modifier
                        .height(8.dp)
                        .align(Alignment.CenterVertically)
                        .weight(1f),
                value = position.value,
                onValueChange = {
                    viewModel.updateTime(it)
                }
        )
        Text(
                text = maxTime.value,
                modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 8.dp)
        )
    }

}

@Composable
fun PlayerControlView(viewModel: MusicPlayViewModel) {
    Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 12.dp)
    ) {
        Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* 执行第一个IconButton的操作 */ }) {
                Icon(
                        painter = painterResource(id = R.mipmap.ic_loop_one),
                        contentDescription = "循环播放"
                )
            }
            IconButton(onClick = { /* 执行第一个IconButton的操作 */ }) {
                Icon(
                        painter = painterResource(id = R.mipmap.ic_left),
                        contentDescription = "上一首"
                )
            }
            IconButton(onClick = { /* 执行第二个IconButton的操作 */ }) {
                Icon(
                        painter = painterResource(id = R.mipmap.ic_play),
                        contentDescription = "播放"
                )
            }
            IconButton(onClick = { /* 执行第三个IconButton的操作 */ }) {
                Icon(
                        painter = painterResource(id = R.mipmap.ic_right),
                        contentDescription = "下一首"
                )
            }
            IconButton(onClick = { /* 执行第三个IconButton的操作 */ }) {
                Icon(
                        painter = painterResource(id = R.mipmap.ic_playlist),
                        contentDescription = "播放列表"
                )
            }
        }
    }

}