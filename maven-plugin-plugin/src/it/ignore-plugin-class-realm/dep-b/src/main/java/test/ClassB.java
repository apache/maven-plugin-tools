package test;

public class ClassB
{

    /**
     * For the test we want to trigger a linkage error when resolving ClassA. However, we want this error to occur when
     * analyzing ClassB and not when loading ClassB itself.
     */
    public void triggerLazyLinkageError(ClassA param)
    {
    }

}
