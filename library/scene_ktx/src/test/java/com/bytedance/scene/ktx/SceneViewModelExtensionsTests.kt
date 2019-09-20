package com.bytedance.scene.ktx

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.scene.*
import com.bytedance.scene.group.GroupScene
import com.bytedance.scene.navigation.NavigationScene
import com.bytedance.scene.navigation.NavigationSceneOptions
import com.bytedance.scene.utlity.ViewIdGenerator
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SceneViewModelExtensionsTests {
    @Test
    fun testSceneScopeViewModel() {
        val scene = object : Scene() {
            private val testViewModel: TestMyViewModel by viewModels()

            override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
                return View(requireSceneContext())
            }

            override fun onActivityCreated(savedInstanceState: Bundle?) {
                super.onActivityCreated(savedInstanceState)
                assertTrue(testViewModel.test())
                assertSame(SceneViewModelProviders.of(this).get(TestMyViewModel::class.java), testViewModel)
            }
        }
        createFromSceneLifecycleManagerByFragmentActivity(scene)
    }

    @Test
    fun testActivityScopeViewModel() {
        val scene = object : Scene() {
            private val testViewModel: TestActivityModel by activityViewModels()

            override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
                return View(requireSceneContext())
            }

            override fun onActivityCreated(savedInstanceState: Bundle?) {
                super.onActivityCreated(savedInstanceState)
                assertTrue(testViewModel.test())
                assertSame(ViewModelProviders.of(requireActivity() as FragmentActivity).get(TestActivityModel::class.java), testViewModel)
            }
        }
        createFromSceneLifecycleManagerByFragmentActivity(scene)
    }

    @Test
    fun testParentSceneScopeViewModel() {
        val groupScene = object : GroupScene() {
            val testViewModel: TestActivityModel by viewModels()
            override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): ViewGroup {
                return FrameLayout(requireSceneContext())
            }

            override fun onActivityCreated(savedInstanceState: Bundle?) {
                super.onActivityCreated(savedInstanceState)
                requireView().id = ViewIdGenerator.generateViewId()
            }
        }
        createFromSceneLifecycleManagerByFragmentActivity(groupScene)
        val scene = object : Scene() {
            val testViewModel: TestActivityModel by viewModels({ requireParentScene() })

            override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
                return View(requireSceneContext())
            }

            override fun onActivityCreated(savedInstanceState: Bundle?) {
                super.onActivityCreated(savedInstanceState)
                assertSame(SceneViewModelProviders.of(requireParentScene()).get(TestActivityModel::class.java), testViewModel)
            }
        }
        groupScene.add(groupScene.requireView().id, scene, "TAG")
        assertSame(groupScene.testViewModel, scene.testViewModel)
    }

    class TestMyViewModel : ViewModel() {
        fun test(): Boolean {
            return true
        }
    }

    class TestActivityModel : ViewModel() {
        fun test(): Boolean {
            return true
        }
    }
}

fun createFromSceneLifecycleManagerByFragmentActivity(rootScene: Scene): NavigationScene {
    val pair = createFromInitSceneLifecycleManagerByFragmentActivity(rootScene)
    val sceneLifecycleManager = pair.first
    sceneLifecycleManager.onStart()
    sceneLifecycleManager.onResume()
    return pair.second
}

fun createFromInitSceneLifecycleManagerByFragmentActivity(rootScene: Scene): Pair<SceneLifecycleManager, NavigationScene> {
    val controller = Robolectric.buildActivity<TestFragmentActivity>(TestFragmentActivity::class.java!!).create().start().resume()
    val testActivity = controller.get()
    val navigationScene = NavigationScene()
    val options = NavigationSceneOptions(rootScene.javaClass)
    navigationScene.setArguments(options.toBundle())

    val navigationSceneHost = object : NavigationScene.NavigationSceneHost {
        override fun isSupportRestore(): Boolean {
            return false
        }

        override fun startActivityForResult(intent: Intent, requestCode: Int) {

        }

        override fun requestPermissions(permissions: Array<String>, requestCode: Int) {

        }
    }

    val rootScopeFactory = Scope.RootScopeFactory { Scope.DEFAULT_ROOT_SCOPE_FACTORY.rootScope }

    val sceneComponentFactory = SceneComponentFactory { cl, className, bundle ->
        if (className == rootScene.javaClass.name) {
            rootScene
        } else null
    }

    navigationScene.defaultNavigationAnimationExecutor = null

    val sceneLifecycleManager = SceneLifecycleManager()
    sceneLifecycleManager.onActivityCreated(testActivity, testActivity.mFrameLayout,
            navigationScene, navigationSceneHost, rootScopeFactory,
            sceneComponentFactory, null)
    return Pair(sceneLifecycleManager, navigationScene)
}

class TestFragmentActivity : FragmentActivity() {
    lateinit var mFrameLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFrameLayout = FrameLayout(this)
        setContentView(mFrameLayout)
    }
}