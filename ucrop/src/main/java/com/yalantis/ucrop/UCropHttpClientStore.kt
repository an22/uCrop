package com.yalantis.ucrop

import okhttp3.OkHttpClient

object UCropHttpClientStore {

    var client: OkHttpClient = OkHttpClient()
        private set

    fun setClient(client: OkHttpClient) {
        this.client = client
    }

}
