package jp.techacademy.yuuki.kawashima.qa_app

import android.content.Intent
import android.graphics.Color
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.list_question_detail.*
import android.widget.ToggleButton

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    private val mFavoriteListener = object : ChildEventListener {

        //ChildAddedはすでにデータがある場合にしか呼ばれない
        //からこのメソッドが呼ばれたときは登録済み
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            favoritebutton.text = "登録済み"
            favoritebutton.setBackgroundColor(Color.YELLOW)
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }


        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

    }

    override fun onResume() {
        super.onResume()

        val user = FirebaseAuth.getInstance().currentUser

        //お気に入りボタンの表示
        if (user == null) {     //ログインしていない場合
            //お気に入りボタン非表示
            favoritebutton.visibility = View.GONE

        } else {               //ログインしている場合

            //追加
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            mFavoriteRef = dataBaseReference.child(FavoritePATH).child(user!!.uid).child(mQuestion.questionUid)
            val data = HashMap<String, String>()

            mFavoriteRef.addChildEventListener(mFavoriteListener)  //追加

            // ボタン表示
            favoritebutton.visibility = View.VISIBLE;

            //お気に入りボタン押した時
            favoritebutton.setOnClickListener {
                if (favoritebutton.text == "お気に入り") {
                    //表示を切り替え
                    favoritebutton.text = "登録済み"
                    favoritebutton.setBackgroundColor(Color.YELLOW)

                    //Fiewbaseに登録
                    data["genre"] = mQuestion.genre.toString()
                    mFavoriteRef.setValue(data)

                } else if (favoritebutton.text == "登録済み") {
                    //表示切り替え
                    favoritebutton.text = "お気に入り"
                    favoritebutton.setBackgroundColor(Color.LTGRAY)

                    //登録削除
                    mFavoriteRef.removeValue()
                }
            }

        }

    }

}