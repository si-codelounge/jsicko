/*
 * Copyright (C) 2019 Andrea Mocci and CodeLounge https://codelounge.si.usi.ch
 *
 * This file is part of jSicko - Java SImple Contract checKer.
 *
 *  jSicko is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * jSicko is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jSicko.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package ch.usi.si.codelounge.jsicko.compilation.utils;

import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

class SimpleSourceFile extends SimpleJavaFileObject {
    private String content;

    public SimpleSourceFile(String qualifiedClassName, String fileName) throws IOException {
        super(URI.create(String.format(
                "file://%s%s", qualifiedClassName.replaceAll("\\.", "/"),
                Kind.SOURCE.extension)), Kind.SOURCE);
        var res = getClass().getClassLoader().getResource(fileName);
        var file = new File(res.getFile());
        content = new String(Files.readAllBytes(file.toPath()));
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return content;
    }
}