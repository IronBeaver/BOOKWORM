package com.bookworm.reader;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.*;
import android.content.*;

/** Host geometry, real-font typography and production preference boundary tests. */
public final class PhysicalShelfTests {
    private static int checks;
    private static void check(boolean value,String message) { checks++; if(!value) throw new AssertionError(message); }
    private static final class Memory extends Context implements SharedPreferences,SharedPreferences.Editor {
        final Map<String,Object> values=new HashMap<>(),pending=new HashMap<>(); boolean fail;
        public SharedPreferences getSharedPreferences(String name,int mode) { check(name.equals("bookshelf"),"Canonical bookshelf preference file");return this; }
        public String getString(String k,String f) { return values.containsKey(k)?(String)values.get(k):f; }
        public boolean getBoolean(String k,boolean f) { return values.containsKey(k)?(Boolean)values.get(k):f; }
        public Editor edit() { pending.clear();return this; }
        public Editor putString(String k,String v) { pending.put(k,v);return this; }
        public Editor putBoolean(String k,boolean v) { pending.put(k,v);return this; }
        public boolean commit() { if(fail) return false; values.putAll(pending);return true; }
    }
    public static void main(String[] args) {
        Memory memory=new Memory(); memory.values.put("sort","RECENT");
        check(BookshelfPreferences.load(memory,false).presentation==PhysicalShelf.Presentation.GRID,"Old grid migrates without writes");
        check(BookshelfPreferences.load(memory,true).presentation==PhysicalShelf.Presentation.COVERS,"Old classic migrates without writes");
        check(memory.values.size()==1,"Migration does not mutate preferences");
        check(BookshelfPreferences.load(memory,false).depth,"Depth default enabled");
        for(PhysicalShelf.Presentation p:PhysicalShelf.Presentation.values()) for(boolean depth:new boolean[]{true,false}) {
            check(new BookshelfPreferences(p,depth).save(memory),"Save selection");
            BookshelfPreferences restored=BookshelfPreferences.load(memory,p==PhysicalShelf.Presentation.GRID);
            check(restored.presentation==p && restored.depth==depth,"Fresh load preserves mode and effects independent of old classic");
            check("RECENT".equals(memory.values.get("sort")),"Save preserves sorting key");
        }
        memory.values.put("presentation","future-mode"); check(BookshelfPreferences.load(memory,true).presentation==PhysicalShelf.Presentation.COVERS,"Unknown mode falls back safely");
        memory.fail=true; check(!new BookshelfPreferences(PhysicalShelf.Presentation.SPINES,false).save(memory),"Commit failure reported");
        PhysicalShelf state=PhysicalShelf.selection(); check(state.selected()==null,"No initial selection");
        state.select("a"); check(state.isSelected("a")&&!state.isSelected("b"),"One selected identity");
        state.select("b"); check(state.isSelected("b")&&!state.isSelected("a"),"Replacing selection clears old highlight");
        state.dismiss(); check(!state.isSelected("b")&&state.selected()==null,"Dismiss clears selection");
        Set<Integer> widths=new HashSet<>(),heights=new HashSet<>();
        for(String id:new String[]{"", "a", "b", "frankenstein", "abc123", "你好", "😀", "Aa", "BB", "MIN_VALUE", "book1", "book2", "book3", "book4", "book5", "book6"}) {
            int w=PhysicalShelf.width(id),h=PhysicalShelf.height(id);widths.add(w);heights.add(h);
            check(w>=56&&w<=76&&h>=210&&h<=258,"Dimensions are bounded and fit 84dp cells");
            check(w==PhysicalShelf.width(id)&&h==PhysicalShelf.height(id),"Dimensions stable across fresh calculations");
            check(PhysicalShelf.columns(600)*84<=600,"Tablet touch cells do not overlap");
        }
        check(widths.size()>3&&heights.size()>3,"Natural variation");
        for(int width:new int[]{240,320,480,600,800,1280}) {
            int columns=PhysicalShelf.columns(width);check(width/columns>=84,"Each cell comfortably exceeds 48dp");
            for(int count:new int[]{0,1,5,6,7,31}) {
                Set<Integer> visited=new HashSet<>();
                for(int r=0;r<PhysicalShelf.rows(count,columns);r++) for(int c=0;c<columns;c++) if(r*columns+c<count) visited.add(r*columns+c);
                check(visited.size()==count,"Odd and full shelf rows retain all sorted records");
            }
        }
        String key=PhysicalShelf.cacheKey("id","Title","Author",1,64,240,1);
        check(key.equals(PhysicalShelf.cacheKey("id","Title","Author",1,64,240,1)),"Cache stable");
        check(!key.equals(PhysicalShelf.cacheKey("id","Changed","Author",1,64,240,1)),"Metadata invalidates cache");
        check(!key.equals(PhysicalShelf.cacheKey("id","Title","Author",2,64,240,1)),"Colour invalidates cache");
        check(!key.equals(PhysicalShelf.cacheKey("id","Title","Author",1,65,240,1)),"Resolution invalidates cache");
        check(!key.equals(PhysicalShelf.cacheKey("id","Title","Author",1,64,240,1.3f)),"Font scale invalidates cache");
        check(!PhysicalShelf.cacheKey("ab","c","",0,64,240,1).equals(PhysicalShelf.cacheKey("a","bc","",0,64,240,1)),"Cache key boundaries unambiguous");
        FontRenderContext frc=new FontRenderContext(null,true,true);
        CoverTypography.Metrics metrics=new CoverTypography.Metrics() {
            Font font(float size) {return new Font("Serif",Font.BOLD,12).deriveFont(size);}
            public float width(String text,float size) {return (float)font(size).getStringBounds(text,frc).getWidth();}
            public float lineHeight(float size) {return font(size).getLineMetrics("Ag",frc).getHeight();}
        };
        for(String title:new String[]{null,"", "Frankenstein; or, The Modern Prometheus", "A Very Long Title ".repeat(80),"Über die Bücher — Ελληνικά", "你好世界の本", "😀".repeat(100)}) {
            for(int w:new int[]{56,64,76}) {
                CoverTypography.Layout layout=SpineTypography.title(title,w,230,1.3f,metrics);
                float unit=w/64f;
                check(!layout.lines.isEmpty()&&layout.lines.size()<=2,"Spine title bounded to two lines");
                check(layout.lines.size()*layout.lineHeight<=30*unit+.01,"Title fits allocated height");
                for(String line:layout.lines) check(metrics.width(line,layout.size)<=230-82*unit+.1,"Long and Unicode title fits width");
            }
        }
        System.out.println("Passed "+checks+" physical shelf geometry/selection/cache/typography/preference checks (host preference adapter, not Android execution).");
    }
}
