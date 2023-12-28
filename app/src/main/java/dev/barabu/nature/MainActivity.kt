package dev.barabu.nature

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import dev.barabu.nature.mountains.MountainsActivity
import dev.barabu.nature.sphere.main.ColSphereActivity
import dev.barabu.nature.sphere.main.TexSphereActivity
import dev.barabu.nature.sphere.planet.PlanetActivity
import dev.barabu.nature.video.distortion.VideoDistortionActivity

class MainActivity : BaseActivity() {

    private lateinit var scrollView: NestedScrollView
    private lateinit var cardsContainer: LinearLayout

    private val stages = mapOf(
        MountainsActivity::class.java to R.string.menu_mountains,
        ColSphereActivity::class.java to R.string.menu_globe_sphere,
        TexSphereActivity::class.java to R.string.menu_main_sphere,
        PlanetActivity::class.java to R.string.menu_planet_sphere,
        VideoDistortionActivity::class.java to R.string.menu_cat_killer,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        findView()
        inflateMenu(cardsContainer, stages)
        hideSystemBars()
    }

    private fun findView() {
        scrollView = findViewById(R.id.scroll_view)
        cardsContainer = findViewById(R.id.cards_container)
    }

    private fun inflateMenu(root: LinearLayout, model: Map<out Class<*>, Int>) {
        val inflater = layoutInflater

        model.entries.forEach { entry ->
            val (clazz, stringId) = entry

            val cardView = inflater.inflate(R.layout.layout_card_main_menu, root, false)
            val titleView = cardView.findViewById<TextView>(R.id.tv_title)
            titleView.text = getString(stringId)

            cardView.setOnClickListener {
                startActivity(Intent(this, clazz))
            }
            root.addView(cardView)
        }
    }
}