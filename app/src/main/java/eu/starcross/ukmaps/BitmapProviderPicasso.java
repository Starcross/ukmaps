package eu.starcross.ukmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.qozix.tileview.graphics.BitmapProvider;
import com.qozix.tileview.tiles.Tile;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.IOException;


public class BitmapProviderPicasso implements BitmapProvider{

    private String base_url;
    private static final String TAG = "UKMaps";
    private static final int tilesize = 200;

    private static final String[][] mapGrid =
            {{""  ,""  ,""  ,""  ,"HP",""  ,""  },
             {""  ,""  ,""  ,"HT","HU",""  ,""  },
             {""  ,"HW","HX","HY","HZ",""  ,""  },
             {"NA","NB","NC","ND",""  ,""  ,""  },
             {"NF","NG","NH","NJ","NK",""  ,""  },
             {"NL","NM","NN","NO",""  ,""  ,""  },
             {""  ,"NR","NS","NT","NU",""  ,""  },
             {""  ,"NW","NX","NY","NZ","OV",""  },
             {""  ,""  ,"SC","SD","SE","TA",""  },
             {""  ,""  ,"SH","SJ","SK","TF","TG"},
             {""  ,"SM","SN","SO","SP","TL","TM"},
             {""  ,"SR","SS","ST","SU","TQ","TR"},
             {"SV","SW","SX","SY","SZ","TV",""  }
            };

    public BitmapProviderPicasso(String url) {
        updateUrl(url);
    }

    public void updateUrl(String url) {
        base_url = url;
    }

    public Bitmap getBitmap(Tile tile, Context context ) {
        Object data = tile.getData();
        if( data instanceof String ) {
            String unformattedFileName = (String) tile.getData();
            //String formattedFileName = String.format( unformattedFileName, tile.getColumn(), tile.getRow() );
            //String formattedFileName = "TL99SW99.gif";
            String formattedFileName = convertFilename(tile.getColumn(), tile.getRow());
            if (formattedFileName == null) {
                return null;
            }
            String path = base_url + formattedFileName;

            Bitmap bitmap = null;
            try {
                //bitmap = Picasso.with(context).load(path).get();
                //Log.i(TAG, "Attempting to get bitmap");
                Picasso p = Picasso.with(context);
                RequestCreator r = p.load(path);
                bitmap = r.get();
                //Log.i(TAG, "Completed get bitmap");
            } catch (IOException e) {
              // Do something
              Log.e(TAG, "Unable to get bitmap:" +  e.getMessage());
            }
            return bitmap;
        }
        return null;
    }

    protected String convertFilename(int column, int row) {
        int x = column % tilesize;
        String grid = mapGrid[row/tilesize][column/tilesize];
        if (grid == "") {
            return null;
        }
        // Switch y axis
        int iRow = (tilesize * mapGrid.length) - row - 1;
        int y = iRow % tilesize;
        // Scale up to 5*
        x *= 5;
        y *= 5;
        String ns = (y%100)>=50 ? "N" : "S";
        String ew = (x%100)>=50 ? "E" : "W";
        String x1 = String.valueOf( x/100 );
        String y1 = String.valueOf( y/100 );
        String x2 = String.valueOf( (x/5)%10 );
        String y2 = String.valueOf( (y/5)%10 );
        String result = String.format("%s/%s%s%s%s%s%s%s.gif", grid, grid, x1, y1, ns, ew, x2, y2);
        return result;
    }

}

