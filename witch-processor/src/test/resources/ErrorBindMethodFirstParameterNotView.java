package com.example.witch.app;

import se.snylt.witch.annotations.Data;
import se.snylt.witch.annotations.Bind;
import android.view.View;

class ErrorBindMethodFirstParameterNotView {

    @Data
    String text = "foo";

    @Bind(id = 0)
    void text(String text, View view) {

    }
}
