
package tech.bison.jackerpot.testdummies;

public class FooHandler {

  FooView view;
  Bar bar;

  public FooHandler() {
    bar = new Bar();
    view = new FooView();
    view.isBar();
  }

  public void anotherMethod() {
    IBarView barExt = view.getBarExt();
  }

  public void useOfExt() {
    bar.isBar();
    view.isBar();
    view.isBaz("gugus");
  }

  private class Bar {
    void isBar() {

    }
  }
}
