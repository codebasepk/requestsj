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

public class HttpFileUploadProgress {
    public File file;
    public long uploaded;
    public long total;
    public int fileNumber;
    public int filesCount;

    public HttpFileUploadProgress(int count) {
        filesCount = count;
    }

    void setCurrentFile(File currentFile) {
        this.file = currentFile;
    }

    void setUploaded(long uploaded) {
        this.uploaded = uploaded;
    }

    void setTotal(long total) {
        this.total = total;
    }

    void setFileNumber(int fileNumber) {
        this.fileNumber = fileNumber;
    }
}
