/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.os.Bundle;
import android.util.Log;

public class ImportBrowseWebObjects_Dirs extends ImportBrowseWebObjects {
    final public static String _ROOT_OBJECTS_URL = "DIRS_ROOT_OBJECTS_URL";
    protected String get_DD_DOC_ROOT() {
        Log.d("ImportBrowseWebObj_Dirs", "ImportBrowseWebObjects_Dirs: get_DD_DOC_ROOT: start");
        return "http://ddp2p.net/objects/directories.html";
    }
    protected String get_ROOT_OBJECTS_URL() {
        return _ROOT_OBJECTS_URL;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ImportBrowseWebObj_Dirs", "ImportBrowseWebObjects_Dirs: onCreate: start");
        super.onCreate(savedInstanceState);
        Log.d("ImportBrowseWebObj_Dirs", "ImportBrowseWebObjects_Dirs: onCreate: done");
    }
}
