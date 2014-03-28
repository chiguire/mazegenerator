 //<>//

PacmanMap leMap;
PFont f;
color wallColor;
color pelletColor;
color energizerColor;

float tileLength = 19.0f;
float cellLength = 25.0f;

void setup() {
  size(900, 650, P3D);
  randomSeed(millis());
  leMap = new PacmanMap();
  leMap.generate();
  background(255);
  
  //println(getStringFromTiles(leMap.tileMap));
  
  wallColor = color(127+random(126), 127+random(126), 127+random(126), 255);
  pelletColor = color(255, 204, 0, 255);
  energizerColor = pelletColor;
  
  f = createFont("Monospaced", 14, false);
  textFont(f, 14);
}

void draw() {
  clear();
  background(255);
  fill(0);
  text("Pacman-like Maze Generator", 10, 30);
  text("Programming: Ciro Duran", 10, 30+15);
  text("Based on code by: Shaun Williams", 10, 30+30);
  
  text("Original cell map", 20, 30+70);
  pushMatrix();
  translate(30, 120);
  drawCellMap(leMap.getCellMap());
  popMatrix();
  
  translate(335, 30);
  if (leMap.tileMap != null) {
    drawTileMap(leMap.tileMap, wallColor, pelletColor, energizerColor, false);
  } else {
    drawTileMap(leMap.getTileMap(), wallColor, pelletColor, energizerColor, false);
  }
}

void drawCellMap(Cell[][] cellMap) {
  float cellSide = cellLength;
  
  stroke(200);
  for (int j = 0; j != cellMap[0].length+1; j++) {
    float y = j*cellSide;
    float x0 = 0;
    float x1 = cellMap.length*cellSide;
    line(x0, y, x1, y);
  }
  for (int i = 0; i != cellMap.length+1; i++) {
    float x = cellSide*i;
    float y0 = 0;
    float y1 = cellMap[0].length*cellSide;
    line(x, y0, x, y1);
  }
  
  stroke(20);
  for (int j = 0; j != cellMap[0].length; j++) {
    for (int i = 0; i != cellMap.length; i++) {
      Cell c = cellMap[i][j];
      float x0 = cellSide*i;
      float y0 = cellSide*j;
      float x1 = cellSide*(i+1);
      float y1 = cellSide*(j+1);
      
      if (!c.connect[Cell.UP]) {
        line(x0, y0, x1, y0);
      }
      
      if (!c.connect[Cell.DOWN]) {
        line(x0, y1, x1, y1);
      }
      
      if (!c.connect[Cell.LEFT]) {
        line(x0, y0, x0, y1);
      }
      
      if (!c.connect[Cell.RIGHT]) {
        line(x1, y0, x1, y1);
      }
      
      if (c.number >= 0) {
        fill(0);
        text(""+c.number, x0+5, (y0+y1)/2 +5);
      }
    }
  }
}

void drawTileMap(Tile[][] tileMap, color wallColor, color pelletColor, color energizerColor, boolean drawHelp) {
  float tileSide = tileLength;
  
  fill(0);
  rect(-15, -15, tileMap.length*tileSide+30, tileMap[0].length*tileSide+30);
  
  if (drawHelp) {
    stroke(200);
    for (int j = 0; j != tileMap[0].length+1; j++) {
      float x0 = 0;
      float y = j*tileSide;
      float x1 = tileMap.length*tileSide;
      line(x0, y, x1, y); 
    }
    
    for (int i = 0; i != tileMap.length; i++) {
      float y0 = 0;
      float x = i*tileSide;
      float y1 = tileMap[0].length*tileSide;
      line(x, y0, x, y1);
    }
  } else {
    noStroke();
  }
  
  //draw walls
  for (int j = 0; j != tileMap[0].length; j++) {
    for (int i = 0; i != tileMap.length; i++) {
      int t11 = getTile(tileMap, i, j);
      boolean isPatht11 = t11 == Tile.PATH || t11 == Tile.ENERGIZER || t11 == Tile.BLANK;
      
      float x = tileSide*i;
      float y = tileSide*j;
      float xMid = x + tileSide/2.0f;
      float yMid = y + tileSide/2.0f;
      
      if (t11 == Tile.PATH) {
        fill(pelletColor);
        ellipse(xMid, yMid, tileSide/4.0f, tileSide/4.0f);
      } else if (t11 == Tile.BLANK || t11 == Tile.GHOSTSPACE) {
        //draw nothing :-)
      } else if (t11 == Tile.ENERGIZER) {
        fill(energizerColor);
        ellipse(xMid, yMid, tileSide/2.0f, tileSide/2.0f);
      } else if (t11 == Tile.GHOSTWALL) {
        fill(255, 255, 255, 255);
        rect(x, yMid+tileSide/6.0f, tileSide, tileSide/5.0f);
      } else if (t11 == Tile.WALL) {
        int tileDrawn = -1;
        
        int t01 = getTile(tileMap, i-1, j);
        int t21 = getTile(tileMap, i+1, j);
        int t10 = getTile(tileMap, i, j-1);
        int t12 = getTile(tileMap, i, j+1);
      
        boolean bp01 = t01 == Tile.PATHBLANK || t01 == Tile.PATH || t01 == Tile.ENERGIZER;
        boolean bp21 = t21 == Tile.PATHBLANK || t21 == Tile.PATH || t21 == Tile.ENERGIZER;
        boolean bp10 = t10 == Tile.PATHBLANK || t10 == Tile.PATH || t10 == Tile.ENERGIZER;
        boolean bp12 = t12 == Tile.PATHBLANK || t12 == Tile.PATH || t12 == Tile.ENERGIZER;
        
        boolean bgo01 = t01 == Tile.BLANK || t01 == Tile.GHOSTSPACE || t01 == -1;
        boolean bgo21 = t21 == Tile.BLANK || t21 == Tile.GHOSTSPACE || t21 == -1;
        boolean bgo10 = t10 == Tile.BLANK || t10 == Tile.GHOSTSPACE || t10 == -1;
        boolean bgo12 = t12 == Tile.BLANK || t12 == Tile.GHOSTSPACE || t12 == -1;
        
        boolean w01 = t01 == Tile.WALL || t01 == Tile.GHOSTWALL;
        boolean w21 = t21 == Tile.WALL || t21 == Tile.GHOSTWALL;
        boolean w10 = t10 == Tile.WALL || t10 == Tile.GHOSTWALL;
        boolean w12 = t12 == Tile.WALL || t12 == Tile.GHOSTWALL;
        
        //Corner case of path in map border
        if (i == 0 || i == tileMap.length-1) {
          //Corners with blank behind
          if ((bgo01 || w01) && bp21 && w10 && w12) {
            tileDrawn = 2;
          } else
          if (bp01 && (bgo21 || w21) && w10 && w12) {
            tileDrawn = 3;
          } else
          //Corners directly to tunnel
          if (t01 == -1 && bp21 && bp10 && w12) {
            tileDrawn = 5;
          } else
          if (bp01 && t21 == -1 && bp10 && w12) {
            tileDrawn = 4;
          } else
          if (bp01 && t21 == -1 && w10 && bp12) {
            tileDrawn = 7;
          } else
          if (t01 == -1 && bp21 && w10 && bp12) {
            tileDrawn = 6;
          } else
          //Inverse corners
          if (t01 == -1 && t21 == Tile.WALL && (t10 == -1 || t10 == Tile.BLANK) && w12) {
            tileDrawn = 11;
          } else
          if (w01 && t21 == -1 && (t10 == -1 || t10 == Tile.BLANK) && w12) {
            tileDrawn = 9;
          } else
          if (w01 && t21 == -1 && w10 && (t12 == -1 || t12 == Tile.BLANK)) {
            tileDrawn = 8;
          } else
          if (t01 == -1 && w21 && w10 && (t12 == -1 || t12 == Tile.BLANK)) {
            tileDrawn = 10;
          } else
          //Corners with wall behind
          if (t01 == -1 && w21 && w10 && w12) {
            int t20 = getTile(tileMap, i+1, j-1);
            int t22 = getTile(tileMap, i+1, j+1);
            boolean w20 = t20 == Tile.WALL || t20 == Tile.BLANK;
            boolean w22 = t22 == Tile.WALL || t22 == Tile.BLANK;
            boolean bp20 = t20 == Tile.PATHBLANK || t20 == Tile.PATH || t20 == Tile.ENERGIZER;
            boolean bp22 = t22 == Tile.PATHBLANK || t22 == Tile.PATH || t22 == Tile.ENERGIZER;
            
            if (w20 && bp22) {
              tileDrawn = 11;
            } else
            if (bp20 && w22) {
              tileDrawn = 10;
            }
          } else
          if (w01 && t21 == -1 && w10 && w12) {
            int t00 = getTile(tileMap, i-1, j-1);
            int t02 = getTile(tileMap, i-1, j+1);
            boolean w00 = t00 == Tile.WALL;
            boolean w02 = t02 == Tile.WALL;
            boolean bp00 = t00 == Tile.PATHBLANK || t00 == Tile.PATH || t00 == Tile.ENERGIZER;
            boolean bp02 = t02 == Tile.PATHBLANK || t02 == Tile.PATH || t02 == Tile.ENERGIZER;
            
            if (w00 && bp02) {
              tileDrawn = 9;
            } else
            if (bp00 && w02) {
              tileDrawn = 8;
            }
          } else
          //Straight walls
          if (t01 == -1 && w21 && bp10 && (bgo12 || w12)) {
            tileDrawn = 0;
          } else
          if (w01 && t21 == -1 && bp10 && (bgo12 || w12)) {
            tileDrawn = 0;
          } else
          if (t01 == -1 && w21 && (bgo10 || w10) && bp12) {
            tileDrawn = 1;
          } else
          if (w01 && t21 == -1 && (bgo10 || w10) && bp12) {
            tileDrawn = 1;
          }
        } else {
          //Tiles not in border columns
          
          //Straight walls
          if (w01 && w21 && bp10 && (bgo12 || w12)) {
            tileDrawn = 0;
          } else
          if (w01 && w21 && (bgo10 || w10) && bp12) {
            tileDrawn = 1;
          } else
          if ((bgo01 || w01) && bp21 && w10 && w12) {
            tileDrawn = 2;
          } else
          if (bp01 && (bgo21 || w21) && w10 && w12) {
            tileDrawn = 3;
          } else
          
          //Corners
          if (bp01 && w21 && bp10 && w12) {
            tileDrawn = 4;
          } else
          if (w01 && bp21 && bp10 && w12) {
            tileDrawn = 5;
          } else
          if (w01 && bp21 && w10 && bp12) {
            tileDrawn = 6;
          } else
          if (bp01 && w21 && w10 && bp12) {
            tileDrawn = 7;
          } else
          {
          //Inverse corners
          //if (w01 && w21 && w10 && w12) {
            int t00 = getTile(tileMap, i-1, j-1);
            int t20 = getTile(tileMap, i+1, j-1);
            int t02 = getTile(tileMap, i-1, j+1);
            int t22 = getTile(tileMap, i+1, j+1);
          
            boolean bp00 = t00 == Tile.PATHBLANK || t00 == Tile.PATH || t00 == Tile.ENERGIZER;
            boolean bp20 = t20 == Tile.PATHBLANK || t20 == Tile.PATH || t20 == Tile.ENERGIZER;
            boolean bp02 = t02 == Tile.PATHBLANK || t02 == Tile.PATH || t02 == Tile.ENERGIZER;
            boolean bp22 = t22 == Tile.PATHBLANK || t22 == Tile.PATH || t22 == Tile.ENERGIZER;
            
            boolean wbgo00 = t00 == Tile.WALL || t00 == Tile.BLANK || t00 == Tile.GHOSTSPACE || t00 == -1;
            boolean wbgo20 = t20 == Tile.WALL || t20 == Tile.BLANK || t20 == Tile.GHOSTSPACE || t20 == -1;
            boolean wbgo02 = t02 == Tile.WALL || t02 == Tile.BLANK || t02 == Tile.GHOSTSPACE || t02 == -1;
            boolean wbgo22 = t22 == Tile.WALL || t22 == Tile.BLANK || t22 == Tile.GHOSTSPACE || t22 == -1;
            
            //boolean w00 = t00 == Tile.WALL;
            //boolean w20 = t20 == Tile.WALL;
            //boolean w02 = t02 == Tile.WALL;
            //boolean w22 = t22 == Tile.WALL;
            
            if (bp00 && wbgo20 && wbgo02 && wbgo22) {
              tileDrawn = 8;
            } else
            if (wbgo00 && wbgo20 && bp02 && wbgo22) {
              tileDrawn = 9;
            } else
            if (wbgo00 && wbgo20 && wbgo02 && bp22) {
              tileDrawn = 11;
            } else
            if (wbgo00 && bp20 && wbgo02 && wbgo22) {
              tileDrawn = 10;
            } else
            { tileDrawn = 12; }
          }
        }
        
        fill(wallColor);
        switch (tileDrawn) {
          case 0:
            rect(x, yMid, tileSide, tileSide/2.0f);
            break;
          case 1:
            rect(x, y, tileSide, tileSide/2.0f);
            break;
          case 2:
            rect(x, y, tileSide/2.0f, tileSide);
            break;
          case 3:
            rect(xMid, y, tileSide/2.0f, tileSide);
            break;
          case 4:
            rect(xMid, yMid, tileSide/2.0f, tileSide/2.0f, tileSide/2.0f, 0, 0, 0);
            break;
          case 5:
            rect(x, yMid, tileSide/2.0f, tileSide/2.0f, 0, tileSide/2.0f, 0, 0);
            break;
          case 6:
            rect(x, y, tileSide/2.0f, tileSide/2.0f, 0, 0, tileSide/2.0f, 0);
            break;
          case 7:
            rect(xMid, y, tileSide/2.0f, tileSide/2.0f, 0, 0, 0, tileSide/2.0f);
            break;
          case 8:
            rect(x, yMid, tileSide/2.0f, tileSide/2.0f);
            rect(xMid, y, tileSide/2.0f, tileSide);
            break;
          case 9:
            rect(x, y, tileSide/2.0f, tileSide/2.0f);
            rect(xMid, y, tileSide/2.0f, tileSide);
            break;
          case 10:
            rect(xMid, yMid, tileSide/2.0f, tileSide/2.0f);
            rect(x, y, tileSide/2.0f, tileSide);
            break;
          case 11:
            rect(xMid, y, tileSide/2.0f, tileSide/2.0f);
            rect(x, y, tileSide/2.0f, tileSide);
            break;
          case 12:
            rect(x, y, tileSide, tileSide, 2, 2, 2, 2);
            break;
        }
        /*
        if (tileDrawn == 10) {
          println("-1: ("+x+", "+y+")");
          println(">>> "+getTile(tileMap, i-1, j-1)+"|"+getTile(tileMap, i+0, j-1)+"|"+getTile(tileMap, i+1, j-1));
          println(">>> "+getTile(tileMap, i-1, j+0)+"|"+getTile(tileMap, i+0, j+0)+"|"+getTile(tileMap, i+1, j+0));
          println(">>> "+getTile(tileMap, i-1, j+1)+"|"+getTile(tileMap, i+0, j+1)+"|"+getTile(tileMap, i+1, j+1)+"\n");
        }*/
        
        if (drawHelp) {
          fill(0, 0, 200);
          text(""+tileDrawn, x+3, yMid+4);
        }
      }
    }
  }
}

int getTile(Tile[][] tM, int x, int y) {
  if (x < 0 || x >= tM.length || y < 0 || y >= tM[0].length) {
    return -1;
  }
  return tM[x][y].state;
}

String getStringFromTiles(Tile[][] tileMap) {
  String s = "";
  for (int j = 0; j != tileMap[0].length; j++) {
    for (int i = 0; i != tileMap.length; i++) {
      Tile t = tileMap[i][j];
      String x = null;
      if (t.state == Tile.WALL) {
        x = "*";
      } else if (t.state == Tile.BLANK) {
        x = " ";
      } else if (t.state == Tile.PATH) {
        x = "·";
      } else if (t.state == Tile.GHOSTWALL) {
        x = "-";
      } else if (t.state == Tile.ENERGIZER) {
        x = "¤";
      }
      s += x;
    }
    s+="\n";
  }
  return s;
}

void mousePressed(MouseEvent e) {
  leMap.generate();
  leMap.getTileMap();
  wallColor = color(127+random(126), 127+random(126), 127+random(126), 255);
  //println(getStringFromTiles(leMap.tileMap));
}
