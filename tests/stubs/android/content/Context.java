package android.content;
/** Host-only preference boundary, never packaged in the app. */
public abstract class Context {
    public static final int MODE_PRIVATE=0;
    public abstract SharedPreferences getSharedPreferences(String name,int mode);
}
