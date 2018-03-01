
package tech.bison.jackerpot.views;

public class FooView {

  public FooExtension getExtension(Class<? extends FooExtension> extensionClass) {
    return new FooExtension();
  }

  public IBarView getBarView(Class<? extends IBarView> clazz) {
    return new BarView();
  }

  @Deprecated
  public IBarView getBarExt() {
    return new BarView();
  }

  @Deprecated
  public boolean isBar() {
    return true;
  }

  @Deprecated
  public boolean isBaz(String buz) {
    return true;
  }
}
