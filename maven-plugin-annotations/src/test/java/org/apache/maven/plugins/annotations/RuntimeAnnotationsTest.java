package org.apache.maven.plugins.annotations;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class RuntimeAnnotationsTest {

  @Rule
  public TestName name = new TestName();

  @Parameter
  private String parameter;



  @Test
  public void parameter() {
    Assert.assertTrue(field().isAnnotationPresent(Parameter.class));
  }

  @Component
  private Object component;

  @Test
  public void component() {
    Assert.assertTrue(field().isAnnotationPresent(Component.class));
  }


  private Field field() {
    try {
      return getClass().getDeclaredField(name.getMethodName());
    }
    catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
    catch (SecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
