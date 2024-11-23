/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.kotlin.common.rendering

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/** Helper functions for OpenGL shaders.  */
object ShaderUtil {

    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type The type of shader to create (e.g., vertex or fragment).
     * @param filename The filename of the asset file to be turned into a shader.
     * @param defineValuesMap The #define values to prepend to the shader source code.
     * @return The shader object handle.
     * @throws IOException If there is an error reading the shader file.
     */
    @Throws(IOException::class)
    fun loadGLShader(
        tag: String?,
        context: Context,
        type: Int,
        filename: String,
        defineValuesMap: Map<String, Int>
    ): Int {
        // Load shader source code.
        var code = readShaderFileFromAssets(context, filename)

        // Prepend any #define values specified.
        val defines = defineValuesMap.entries.joinToString(separator = "\n") { (key, value) ->
            "#define $key $value"
        }
        code = defines + "\n" + code

        // Compile shader code.
        var shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)

        // Check compilation status.
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(tag, "Error compiling shader: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            shader = 0
        }

        if (shader == 0) {
            throw RuntimeException("Error creating shader.")
        }

        return shader
    }

    /**
     * Overload of `loadGLShader` that assumes no additional #define values to add.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun loadGLShader(tag: String?, context: Context, type: Int, filename: String): Int {
        return loadGLShader(tag, context, type, filename, emptyMap())
    }

    /**
     * Checks if there is an error in OpenGL ES operations.
     *
     * @param label Label to report in case of error.
     * @throws RuntimeException If an OpenGL error is detected.
     */
    @JvmStatic
    fun checkGLError(tag: String?, label: String) {
        var lastError = GLES20.GL_NO_ERROR
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e(tag, "$label: glError $error")
            lastError = error
        }
        if (lastError != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$label: glError $lastError")
        }
    }

    /**
     * Reads a raw shader file from the assets folder and returns its content as a string.
     *
     * @param filename The filename of the shader file.
     * @return The content of the shader file.
     * @throws IOException If there is an error reading the file.
     */
    @Throws(IOException::class)
    private fun readShaderFileFromAssets(context: Context, filename: String): String {
        context.assets.open(filename).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Handle #include directives.
                    val tokens = line!!.trim().split("\\s+".toRegex())
                    if (tokens.isNotEmpty() && tokens[0] == "#include") {
                        if (tokens.size < 2) {
                            throw IOException("Invalid #include directive: $line")
                        }
                        val includeFilename = tokens[1].replace("\"", "")
                        if (includeFilename == filename) {
                            throw IOException("Circular inclusion detected in file: $filename")
                        }
                        sb.append(readShaderFileFromAssets(context, includeFilename))
                    } else {
                        sb.append(line).append("\n")
                    }
                }
                return sb.toString()
            }
        }
    }
}
