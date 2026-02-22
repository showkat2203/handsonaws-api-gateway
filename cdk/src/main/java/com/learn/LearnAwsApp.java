package com.learn;

import com.learn.stacks.Phase1Stack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class LearnAwsApp {

    public static void main(final String[] args) {
        App app = new App();

        new Phase1Stack(app, "LearnApigwPhase1", StackProps.builder().build());

        app.synth();
    }
}
