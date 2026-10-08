package hu.ae6820dc.apktest

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)

  val root = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   gravity = Gravity.CENTER
   setPadding(48, 48, 48, 48)
   setBackgroundColor(Color.rgb(11, 18, 32))
  }

  val title = TextView(this).apply {
   text = "APK GYÁR MŰKÖDIK!"
   textSize = 27f
   setTextColor(Color.WHITE)
   gravity = Gravity.CENTER
  }

  val sub = TextView(this).apply {
   text = "\nElső natív Android tesztprogram\n\nae6820dc • Android műhely"
   textSize = 17f
   setTextColor(Color.rgb(170, 185, 210))
   gravity = Gravity.CENTER
  }

  root.addView(title)
  root.addView(sub)
  setContentView(root)
 }
}
