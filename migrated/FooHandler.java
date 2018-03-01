package com.yourorganization.maven_sample;

public class FooHandler {

    FooView view = new FooView();

    Bar bar = new Bar();

    public FooHandler() {
        view.isBar();
    }

    public void anotherMethod() {
        IBarView barExt = view.getBarView(IBarView.class);
    }

    public void useOfExt() {
        view.isBar();
        bar.isBar();
        view.isBaz("gugus");
    }

    private class Bar {

        void isBar() {
        }
    }
}
