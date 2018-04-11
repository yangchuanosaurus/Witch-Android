package com.example.witch.app;

import android.view.View;
import se.snylt.witch.annotations.Bind;
import se.snylt.witch.annotations.Data;

class SimpleBindMethodViewDataData {

    @Data
    String text() {
        return "foo";
    }

    @Bind(id = 0)
    void text(View view, String data, String history) {}
}
