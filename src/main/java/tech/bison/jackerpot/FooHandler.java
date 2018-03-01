
package tech.bison.jackerpot;

import tech.bison.jackerpot.views.FooView;
import tech.bison.jackerpot.views.IBarView;

public class FooHandler {

  FooView view = new FooView();
  Bar bar = new Bar();

  public FooHandler() {
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
