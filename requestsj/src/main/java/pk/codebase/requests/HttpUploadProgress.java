/*
 * Requests for Android
 * Copyright (C) 2016-2019 CodeBasePK
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pk.codebase.requests;

import java.io.File;

public class HttpUploadProgress {
    public File currentFile;
    public long upload;
    public long total;
    public int currentNumber;
    public int filesCount;

    public HttpUploadProgress(int count) {
        filesCount = count;
    }

    void setCurrentFile(File currentFile) {
        this.currentFile = currentFile;
    }

    void setUpload(long upload) {
        this.upload = upload;
    }

    void setTotal(long total) {
        this.total = total;
    }

    void setCurrentNumber(int currentNumber) {
        this.currentNumber = currentNumber;
    }
}
