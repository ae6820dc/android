package io.github.ae6820dc.sajatmenu;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.text.Collator;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final String STORE = "c2de45aa7f819b03";
    static final int BG = 0xff101722, CARD = 0xff1c2738, TEXT = 0xffedf3ff, MUTED = 0xffa3b3c9, ACCENT = 0xff72e2c0;
    final ArrayList<String> selected = new ArrayList<>();
    final ArrayList<App> installed = new ArrayList<>();
    final HashMap<String, App> byId = new HashMap<>();
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    GridView grid;
    LinearLayout root;
    TextView count, empty;
    boolean loading = true;
    int scanGeneration = 0;
    static class App {
        String id, name;
        ResolveInfo info;
        App(String id, String name, ResolveInfo info) { this.id=id; this.name=name; this.info=info; }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            JSONArray a = new JSONArray(getPreferencesStore().getString("items", "[]"));
            for(int i=0;i<a.length();i++) {
                String id=a.getString(i);
                if(ComponentName.unflattenFromString(id)!=null && !selected.contains(id)) selected.add(id);
            }
        } catch(Exception ignored) { }
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(20),dp(16),dp(20),dp(12));
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            int left,top,right,bottom;
            if(android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                left=bars.left; top=bars.top; right=bars.right; bottom=bars.bottom;
            } else {
                left=insets.getSystemWindowInsetLeft(); top=insets.getSystemWindowInsetTop();
                right=insets.getSystemWindowInsetRight(); bottom=insets.getSystemWindowInsetBottom();
            }
            v.setPadding(dp(20)+left,dp(16)+top,dp(20)+right,dp(12)+bottom); return insets;
        });
        setContentView(root);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleBox=new LinearLayout(this); titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(text("SAJÁT ALKALMAZÁSAID",11,ACCENT));
        titleBox.addView(text("Saját Menü",30,TEXT));
        header.addView(titleBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView more=text("⋮",32,TEXT); more.setGravity(Gravity.CENTER); more.setContentDescription("Menü");
        more.setBackground(round(CARD,18)); more.setOnClickListener(v->showMenu(more));
        header.addView(more,new LinearLayout.LayoutParams(dp(52),dp(52))); root.addView(header);
        count=text("Alkalmazások betöltése…",14,MUTED); count.setPadding(0,dp(14),0,dp(18)); root.addView(count);
        FrameLayout body=new FrameLayout(this); root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        grid=new GridView(this); grid.setNumColumns(getResources().getConfiguration().screenWidthDp>=600?5:3);
        grid.setHorizontalSpacing(dp(10)); grid.setVerticalSpacing(dp(14)); grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setClipToPadding(false); body.addView(grid);
        empty=text("Még üres a saját menüd.\n\nA jobb felső ⋮ menüben válaszd az\n„Alkalmazás hozzáadása” lehetőséget.",17,MUTED);
        empty.setGravity(Gravity.CENTER); empty.setLineSpacing(dp(4),1); body.addView(empty,new FrameLayout.LayoutParams(-1,-1));
        TextView footer=text("Program ID: 7a93c1e4b8056fd2",10,MUTED); footer.setPadding(0,dp(12),0,0); root.addView(footer);
        render();
    }
    SharedPreferences getPreferencesStore() { return getSharedPreferences(STORE, MODE_PRIVATE); }
    void save() { getPreferencesStore().edit().putString("items",new JSONArray(selected).toString()).apply(); }
    @Override protected void onResume() { super.onResume(); scan(); }
    @Override protected void onDestroy() { scanGeneration++; worker.shutdownNow(); super.onDestroy(); }
    void scan() {
        final int generation=++scanGeneration;
        worker.execute(()-> {
            ArrayList<App> result=new ArrayList<>();
            boolean failed=false;
            try {
                Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                HashSet<String> seen=new HashSet<>();
                for(ResolveInfo r:getPackageManager().queryIntentActivities(query,0)) {
                    if(!r.activityInfo.exported || getPackageName().equals(r.activityInfo.packageName)) continue;
                    String id=new ComponentName(r.activityInfo.packageName,r.activityInfo.name).flattenToString();
                    if(seen.add(id)) result.add(new App(id,r.loadLabel(getPackageManager()).toString(),r));
                }
                Collator order=Collator.getInstance(new Locale("hu","HU"));
                result.sort((a,b)->order.compare(a.name,b.name));
            } catch(Exception e) { failed=true; }
            final boolean error=failed;
            runOnUiThread(()-> {
                if(isFinishing() || isDestroyed() || generation!=scanGeneration) return;
                loading=false;
                if(!error) { installed.clear(); installed.addAll(result); byId.clear(); for(App a:result) byId.put(a.id,a); }
                else Toast.makeText(this,"Nem sikerült beolvasni az alkalmazásokat. Próbáld újra a menüből.",Toast.LENGTH_LONG).show();
                render();
            });
        });
    }
    void showMenu(View anchor) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(0,1,0,"Alkalmazás hozzáadása");
        menu.getMenu().add(0,2,1,"Átrendezés").setEnabled(!selected.isEmpty());
        menu.getMenu().add(0,3,2,"Eltávolítás a menüből").setEnabled(!selected.isEmpty());
        menu.getMenu().add(0,4,3,"Alkalmazáslista frissítése");
        menu.setOnMenuItemClickListener(item->{
            switch(item.getItemId()) { case 1: showPicker(); break; case 2: showReorder(); break; case 3: showRemove(); break; case 4: scan(); break; }
            return true;
        }); menu.show();
    }
    static String normalize(String s) { return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT); }
    AlertDialog picker;
    ListView pickerList;
    void showPicker() {
        if(loading) { Toast.makeText(this,"Az alkalmazáslista még töltődik…",Toast.LENGTH_SHORT).show(); return; }
        ArrayList<App> available=new ArrayList<>(); for(App a:installed) if(!selected.contains(a.id)) available.add(a);
        if(available.isEmpty()) { new AlertDialog.Builder(this).setMessage("Nincs több hozzáadható alkalmazás.").setPositiveButton("Rendben",null).show(); return; }
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(8),dp(16),0);
        EditText search=new EditText(this); search.setSingleLine(true); search.setHint("Keresés az alkalmazások között"); search.setInputType(android.text.InputType.TYPE_CLASS_TEXT); box.addView(search);
        pickerList=new ListView(this); box.addView(pickerList,new LinearLayout.LayoutParams(-1,dp(330)));
        TextView noResult=text("Nincs találat.",15,MUTED); noResult.setPadding(dp(8),dp(12),0,dp(12)); box.addView(noResult); pickerList.setEmptyView(noResult);
        ArrayList<App> shown=new ArrayList<>(available); BaseAdapter adapter=appAdapter(shown,false); pickerList.setAdapter(adapter);
        picker=new AlertDialog.Builder(this).setTitle("Alkalmazás hozzáadása").setView(box).setNegativeButton("Mégse",null).create();
        pickerList.setOnItemClickListener((p,v,position,id)-> { String component=shown.get(position).id; if(!selected.contains(component)) selected.add(component); save(); render(); picker.dismiss(); });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { shown.clear(); String q=normalize(s.toString().trim()); for(App a:available) if(normalize(a.name).contains(q)) shown.add(a); adapter.notifyDataSetChanged(); }
            public void afterTextChanged(Editable e) { }
        }); picker.show(); picker.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    String label(String id) { App a=byId.get(id); if(a!=null) return a.name; ComponentName c=ComponentName.unflattenFromString(id); return (c==null?id:c.getPackageName())+" (nem elérhető)"; }
    String[] labels() { String[] result=new String[selected.size()]; for(int i=0;i<result.length;i++) result[i]=label(selected.get(i)); return result; }
    void showRemove() {
        boolean[] checked=new boolean[selected.size()]; final ArrayList<String> snapshot=new ArrayList<>(selected);
        new AlertDialog.Builder(this).setTitle("Eltávolítás a menüből").setMultiChoiceItems(labels(),checked,(d,i,value)->checked[i]=value)
            .setNegativeButton("Mégse",null).setPositiveButton("Eltávolítás",(d,w)-> {
                for(int i=0;i<snapshot.size();i++) if(checked[i]) selected.remove(snapshot.get(i)); save(); render();
            }).show();
    }
    void showReorder() {
        if(selected.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("Átrendezés – válassz alkalmazást").setItems(labels(),(d,index)-> {
            final String component=selected.get(index);
            String[] positions=new String[selected.size()]; for(int i=0;i<positions.length;i++) positions[i]=(i+1)+". hely";
            new AlertDialog.Builder(this).setTitle(label(component)+" – új hely").setSingleChoiceItems(positions,index,(dialog,position)-> {
                selected.remove(component); selected.add(Math.min(position,selected.size()),component); save(); render(); dialog.dismiss();
            }).setNegativeButton("Mégse",null).show();
        }).setNegativeButton("Mégse",null).show();
    }
    void render() {
        count.setText(loading?"Alkalmazások betöltése…":selected.size()+" alkalmazás a saját menüdben");
        empty.setVisibility(selected.isEmpty()?View.VISIBLE:View.GONE);
        ArrayList<App> apps=new ArrayList<>(); for(String id:selected) { App a=byId.get(id); apps.add(a!=null?a:new App(id,label(id),null)); }
        grid.setAdapter(appAdapter(apps,true));
        grid.setOnItemClickListener((p,v,position,id)->launch(apps.get(position)));
    }
    void launch(App app) {
        try { Intent intent=Intent.makeMainActivity(ComponentName.unflattenFromString(app.id)); startActivity(intent); }
        catch(Exception e) { new AlertDialog.Builder(this).setTitle("Nem indítható").setMessage("Ez az alkalmazás most nem érhető el. A ⋮ menüben eltávolíthatod vagy frissítheted az alkalmazáslistát.").setPositiveButton("Rendben",null).show(); }
    }
    BaseAdapter appAdapter(ArrayList<App> apps, boolean tiles) {
        return new BaseAdapter() {
            public int getCount() { return apps.size(); }
            public Object getItem(int p) { return apps.get(p); }
            public long getItemId(int p) { return p; }
            public View getView(int p,View old,ViewGroup parent) {
                App a=apps.get(p);
                LinearLayout cell=new LinearLayout(MainActivity.this); cell.setOrientation(tiles?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL); cell.setGravity(Gravity.CENTER_VERTICAL);
                cell.setPadding(dp(10),dp(14),dp(10),dp(14)); if(tiles) { cell.setGravity(Gravity.CENTER); cell.setBackground(round(CARD,20)); }
                ImageView icon=new ImageView(MainActivity.this);
                try { icon.setImageDrawable(a.info==null?getPackageManager().getDefaultActivityIcon():a.info.loadIcon(getPackageManager())); } catch(Exception e) { icon.setImageDrawable(getPackageManager().getDefaultActivityIcon()); }
                cell.addView(icon,new LinearLayout.LayoutParams(dp(tiles?48:36),dp(tiles?48:36)));
                TextView name=text(a.name,tiles?13:16,TEXT); name.setMaxLines(2); name.setEllipsize(TextUtils.TruncateAt.END); name.setGravity(tiles?Gravity.CENTER:Gravity.CENTER_VERTICAL); name.setPadding(tiles?0:dp(14),tiles?dp(10):0,0,0);
                cell.addView(name,new LinearLayout.LayoutParams(tiles?-1:0,tiles?dp(48):-2,tiles?0:1)); cell.setContentDescription(a.name); return cell;
            }
        };
    }
    TextView text(String s,float size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    GradientDrawable round(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
}
