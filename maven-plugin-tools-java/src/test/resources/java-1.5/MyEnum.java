public enum MyEnum
{

    @Deprecated()
    SOME_VALUE,

    // cf. MPLUGIN-151
    @SuppressWarnings("all")
    ANOTHER_VALUE,

    @SuppressWarnings(value = { "all" })
    YET_ANOTHER_VALUE;

}
