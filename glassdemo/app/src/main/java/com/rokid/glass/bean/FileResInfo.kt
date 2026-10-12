package com.rokid.glass.bean

class FileResInfo {

    companion object{
        var AlbumDirPath = "/Album/"
    }

    var fileName = ""

    var fileUrl= ""

    // SyncState中常量
    // Constants from SyncState.
    var syncState = ""

    // FileType中常量
    // Constants from FileType.
    var fileType :String?= null

    var glassFilePath= ""
    var phoneFilePath= ""
    var fileMd5:String?= null

    var message = ""


}




object SyncState{


    // glass->phone 同步中
    // glass->phone: synchronization in progress.
    val SyncIng = "SyncIng"

    // glass->phone 本地同步成功
    // glass->phone: local synchronization succeeded.
    val SyncNativeSuccess= "SyncNativeSuccess"
    // glass->phone 本地同步失败
    // glass->phone: local synchronization failed.
    val SyncNativeFailed= "SyncNativeFailed"

    // phone->net 同步服务端成功
    // phone->net: synchronization to the server succeeded.
    val SyncSuccess= "SyncNetSuccess"
    // phone->net 同步服务端成功
    // phone->net: synchronization to the server succeeded.
    val SyncFailed= "SyncFailed"


}




object FileType{


    val ALBUM = "ALBUM"

}


