package com.example.hearts

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_start_game).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        findViewById<Button>(R.id.btn_rules).setOnClickListener {
            showRules()
        }
    }

    private fun showRules() {
        val rules = """
            红心大战 Hearts

            目标：避免吃到红心和黑桃Q，
            获得最低分数！

            计分规则：
            - 每张红心：1分
            - 黑桃Q（♠Q）：13分
            - 全收（射月）：自己0分，其余玩家各26分

            玩法：
            1. 每人13张牌
            2. 每轮开始前传3张牌给其他玩家
            3. 持有♣2的玩家先出牌
            4. 必须跟出相同花色
            5. 没有该花色则出任意牌
            6. 红心不能作为首轮首张
            7. 有人吃到红心后才能出红心
            8. 第一墩不能出红心或♠Q
            9. 13墩结束后计算分数
            10. 有人达到100分时游戏结束
            11. 分数最低者获胜！
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("游戏规则")
            .setMessage(rules)
            .setPositiveButton("知道了", null)
            .show()
    }
}
