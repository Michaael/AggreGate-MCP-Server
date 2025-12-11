package copier

import java.io.File
import java.io.IOException

object ResourceCopier {
    private const val cresDir = "./aggregate-api/src/main/java/com/tibbo/aggregate/common/res/"
    private val frontendSrcDirs: Array<String> = arrayOf("./frontend/typescript-api/src", "./frontend/dashboard-sdk/src/libs/res")

    fun copyResources() {
        try {
            val cresFiles = File(cresDir).list()

            frontendSrcDirs.forEach { dstDir ->
                cresFiles.forEach { cresFileName ->
                    val language = cresFileName.substring(cresFileName.indexOf("_") + 1, cresFileName.indexOf("."))

                    val res = File("$dstDir/res_$language.ts").bufferedWriter()

                    res.appendln("// GENERATED AUTOMATICALLY DO NOT CHANGE!!!").appendln("globalThis.resources = {")

                    File("$cresDir/$cresFileName").bufferedReader().forEachLine {
                        if (it.trim().isNotEmpty()) {
                            val key = it.substringBefore("=").trim()
                            val value = it.substringAfter("=").removePrefix(" ")
                            val quote = if (value.contains("\'")) "\"" else "\'"

                            res.appendln("  $key: $quote$value$quote,")
                        }
                    }
                    res.appendln("};")
                    res.close()
                }
            }
        } catch (ex: IOException) {
            println("Error when copying resources: " + ex.message)
            ex.printStackTrace()
        }
    }
}
