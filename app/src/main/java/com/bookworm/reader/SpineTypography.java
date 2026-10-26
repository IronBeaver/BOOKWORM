package com.bookworm.reader;

/** Reuses the unchanged Bindery text fitter with a bounded two-line spine area. */
public final class SpineTypography {
    private SpineTypography() { }
    public static CoverTypography.Layout title(String title,int width,int height,float fontScale,CoverTypography.Metrics metrics) {
        float unit=width/64f;
        return CoverTypography.fit(title,"Untitled",Math.max(1,height-82*unit),30*unit,
                10*unit,13*unit*Math.min(1.35f,Math.max(1,fontScale)),metrics);
    }
}
