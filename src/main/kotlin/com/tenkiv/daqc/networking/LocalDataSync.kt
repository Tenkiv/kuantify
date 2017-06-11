package com.tenkiv.daqc.networking

import com.couchbase.lite.JavaContext
import com.couchbase.lite.Manager
import com.couchbase.lite.listener.Credentials
import com.couchbase.lite.listener.LiteListener
import com.couchbase.lite.listener.LiteServer
import java.net.URL

/**
 * Created by tenkiv on 5/31/17.
 */
class LocalDataSync {
    val context = JavaContext()
    val manager = Manager(context, Manager.DEFAULT_OPTIONS)
    val db = manager.getDatabase("localstore")
    val doc = db.createDocument()
    val url = URL("http://localhost:8000/localstore")
    val push = db.createPushReplication(url)
    val pull = db.createPullReplication(url)

    init {
        val lis = LiteListener(manager, 8000, Credentials("", ""))

        lis.start()

        val serv = LiteServer()
        serv.setManager(manager)
        serv.setListener(lis)

        pull.isContinuous = true
        push.isContinuous = true

        pull.start()
        push.start()

        db.open()

        doc.addChangeListener(::println)

        doc.putProperties(mapOf(Pair("this", "that")))

        Thread.sleep(10000)

    }
}