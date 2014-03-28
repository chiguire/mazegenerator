import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

class IntTuple {
  public int x;
  public int y;

  public IntTuple(int _x, int _y) {
    x = _x;
    y = _y;
  }

  public IntTuple() {
    this(0, 0);
  }
}

class Tile {
  public IntTuple position;
  public int state;
  public Cell cell;
  
  public static final int BLANK = 0;
  public static final int PATH = 1;
  public static final int PATHBLANK = 2;
  public static final int WALL = 3;
  public static final int GHOSTWALL = 4;
  public static final int ENERGIZER = 5;
  public static final int GHOSTSPACE = 6;
  
  public Tile(int _x, int _y) {
    position = new IntTuple(_x, _y);
    state = BLANK;
    cell = null;
  }
}

class Cell {
  static int numFilled = 0;
  public boolean filled;
  public IntTuple position;
  public boolean connect[];
  public Cell next[];
  public int number;
  public int group;
  
  public boolean isGhostSpace;
  
  public boolean isRaiseHeightCandidate;
  public boolean raiseHeight;
  public boolean isShrinkWidthCandidate;
  public boolean shrinkWidth;

  public boolean isJoinCandidate;

  public boolean isEdgeTunnelCandidate;
  public boolean isVoidTunnelCandidate;
  public boolean isSingleDeadEndCandidate;
  public int singleDeadEndDir;
  public boolean isDoubleDeadEndCandidate;
  public boolean topTunnel;

  public IntTuple tilePosition;
  public IntTuple tileSize;

  static final int UP = 0;
  static final int RIGHT = 1;
  static final int DOWN = 2;
  static final int LEFT = 3;

  public Cell(int _x, int _y) {
    filled = false;
    position = new IntTuple(_x, _y);
    connect = new boolean[] { false, false, false, false };
    next = new Cell[] { null, null, null, null };
    number = -1;
    group = -1;
    
    isGhostSpace = false;

    isRaiseHeightCandidate = false;
    raiseHeight = false;
    isShrinkWidthCandidate = false;
    shrinkWidth = false;

    isJoinCandidate = false;

    isEdgeTunnelCandidate = false;
    isVoidTunnelCandidate = false;
    isSingleDeadEndCandidate = false;
    singleDeadEndDir = -1;
    isDoubleDeadEndCandidate = false;
    topTunnel = false;

    tilePosition = new IntTuple();
    tileSize = new IntTuple();
  }

  public boolean isOpen(int dir, int prevDir, int size) {
    if ((position.y == 6 && position.x == 0 && dir == DOWN)
        || (position.y == 7 && position.x == 0 && dir == UP)) {
      return false;
    }

    if (size == 2 && (dir == prevDir || ((dir + 2) % 4) == prevDir)) {
      return false;
    }

    if (next[dir] != null && !next[dir].filled) {
      if (!(next[dir].next[LEFT] != null && !next[dir].next[LEFT].filled)) {
        return true;
      }
    }

    return false;
  }

  public boolean isOpen(int dir) {
    return isOpen(dir, -1, -1);
  }

  public ArrayList<Integer> getOpenCells(int prevDir, int size) {
    ArrayList<Integer> openCells = new ArrayList<Integer>();
    for (int i = 0; i != 4; i++) {
      if (isOpen(i, prevDir, size)) {
        openCells.add(new Integer(i));
      }
    }
    return openCells;
  }

  public void connect(int dir) {
    connect[dir] = true;
    next[dir].connect[(dir + 2) % 4] = true;
    if (position.x == 0 && dir == Cell.RIGHT) {
      connect[Cell.LEFT] = true;
    }
  }

  public void fill(int numGroup) {
    filled = true;
    number = Cell.numFilled++;
    group = numGroup;
  }

  public boolean isCrossCenter() {
    return connect[UP] && connect[RIGHT] && connect[DOWN] && connect[LEFT];
  }
}

class PacmanMap {
  static Random r = new Random();
  
  private IntTuple cellMapSize;
  // private IntTuple tileMapSize;
  private Cell[][] cellMap;
  public Tile[][] tileMap;

  private int[] tallRows;
  private int[] narrowCols;

  //static Random r = new Random();

  public PacmanMap() {
    cellMapSize = new IntTuple(5, 9);
    // tileMapSize = new IntTuple(28, 31);

    reset();

  }

  private int genCount = 0;
  public void generate() {
    while (true) {
      reset();
      //System.out.println("Generation Attempt #"+genCount);
      attemptGenerate();
      genCount++;
      if (!isDesirable()) {
        //System.out.println("Not desirable");
        continue;
      }

      setUpScaleCoords();
      joinWalls();
      if (!createTunnels()) {
        //System.out.println("Could not create tunnels");
        continue;
      }

      break;
    }
  }

  public Cell[][] getCellMap() {
    return cellMap;
  }
  
  public Tile[][] getTileMap() {
    Tile[][] result = null;
    IntTuple sub = new IntTuple(cellMapSize.x*3 - 1 + 2, cellMapSize.y*3 + 1 + 3);
    int midX = sub.x - 2;
    int fullX = (sub.x-2) * 2;
    
    result = new Tile[fullX][sub.y];
    for (int j = 0; j != sub.y; j++) {
      for (int i = 0; i != fullX; i++) {
        result[i][j] = new Tile(i, j);
      }
    }
    
    for (int j = 0; j != cellMapSize.y; j++) {
      for (int i = 0; i != cellMapSize.x; i++) {
        Cell c = cellMap[i][j];
        
        for (int x0 = 0; x0 < c.tileSize.x; x0++) {
          for (int y0 = 0; y0 < c.tileSize.y; y0++) {
            result[c.tilePosition.x+x0][c.tilePosition.y+1+y0].cell = c;
            if (c.isGhostSpace) { setTile(result, c.tilePosition.x+x0, c.tilePosition.y+y0, Tile.GHOSTSPACE, sub.x, sub.y, midX); }
            //if (c.isGhostSpace) { result[c.tilePosition.x+x0][c.tilePosition.y+1+y0].state = Tile.GHOSTSPACE; } 
          }
        }
      }
    }
    
    for (int j = 0; j < sub.y; j++) {
      for (int i = 0; i < sub.x ; i++) {
        Cell c = result[i][j] != null? result[i][j].cell: null;
        Cell cl = i>0? result[i-1][j].cell: null;
        Cell cu = j>0? result[i][j-1].cell: null;
        
        if (c != null) {
          if (cl != null && c.group != cl.group ||
              cu != null && c.group != cu.group ||
              cu == null && !c.connect[Cell.UP]) {
            setTile(result, i, j, Tile.PATH, sub.x, sub.y, midX);
          }
        } else {
          if (cl != null && 
              (!cl.connect[Cell.RIGHT] || getTileState(result, i-1, j, sub.x, sub.y, midX) == Tile.PATH) ||
              cu != null &&
              (!cu.connect[Cell.DOWN] || getTileState(result, i, j-1, sub.x, sub.y, midX) == Tile.PATH)) {
            setTile(result, i, j, Tile.PATH, sub.x, sub.y, midX);
          }
        }

        if (getTileState(result, i-1, j, sub.x, sub.y, midX) == Tile.PATH &&
            getTileState(result, i, j-1, sub.x, sub.y, midX) == Tile.PATH &&
            getTileState(result, i-1, j-1, sub.x, sub.y, midX) == Tile.BLANK) {
          setTile(result, i, j, Tile.PATH, sub.x, sub.y, midX);
        }
      }
    }
    
    for (Cell c = cellMap[cellMapSize.x-1][0]; c != null; c = c.next[Cell.DOWN]) {
      if (c.topTunnel) {
        int y = c.tilePosition.y + 1;
        setTile(result, sub.x-1, y, Tile.PATH, sub.x, sub.y, midX);
        setTile(result, sub.x-2, y, Tile.PATH, sub.x, sub.y, midX); 
      }
    }
    
    for (int j = 0; j < sub.y; j++) {
      for (int i = 0; i < sub.x; i++) {
        if (getTileState(result, i, j, sub.x, sub.y, midX) != Tile.PATH &&
             (getTileState(result, i-1, j, sub.x, sub.y, midX) == Tile.PATH ||
              getTileState(result, i, j-1, sub.x, sub.y, midX) == Tile.PATH ||
              getTileState(result, i+1, j, sub.x, sub.y, midX) == Tile.PATH ||
              getTileState(result, i, j+1, sub.x, sub.y, midX) == Tile.PATH ||
              getTileState(result, i-1, j-1, sub.x, sub.y, midX) == Tile.PATH ||
              getTileState(result, i+1, j-1, sub.x, sub.y, midX) == Tile.PATH ||
              getTileState(result, i+1, j+1, sub.x, sub.y, midX) == Tile.PATH ||
              getTileState(result, i-1, j+1, sub.x, sub.y, midX) == Tile.PATH)) {
          setTile(result, i, j, Tile.WALL, sub.x, sub.y, midX);
        }
      }
    }
    
    setTile(result, 2, 12, Tile.GHOSTWALL, sub.x, sub.y, midX);
    
    float []range = null;
    if ((range = getTopEnergizerRange(result, sub.x, sub.y, midX)) != null) {
      int y = (int)Math.floor(range[0]+r.nextFloat()*range[1]);
      int x = sub.x - 2;
      setTile(result, x, y, Tile.ENERGIZER, sub.x, sub.y, midX); 
    }
    
    if ((range = getBotEnergizerRange(result, sub.x, sub.y, midX)) != null) {
      int y = (int)Math.floor(range[0]+r.nextFloat()*range[1]);
      int x = sub.x - 2;
      setTile(result, x, y, Tile.ENERGIZER, sub.x, sub.y, midX);
    }
    
    for (int y = 0; y < sub.y; y++) {
      int x = sub.x - 1;
      if (getTileState(result, x, y, sub.x, sub.y, midX) == Tile.PATH) {
        eraseUntilIntersection(result, x, y, sub.x, sub.y, midX);
      }
    }
    
    setTile(result, 1, sub.y-8, Tile.PATHBLANK, sub.x, sub.y, midX);
    
    for (int i = 0; i < 7; i++) {
      int y = sub.y-14;
      setTile(result, i, y, Tile.PATHBLANK, sub.x, sub.y, midX);
      int j = 1;
      while (getTileState(result, i, y+j, sub.x, sub.y, midX) == Tile.PATH &&
             getTileState(result, i-1, y+j, sub.x, sub.y, midX) == Tile.WALL &&
             getTileState(result, i+1, y+j, sub.x, sub.y, midX) == Tile.WALL) {
        setTile(result, i, y+j, Tile.PATHBLANK, sub.x, sub.y, midX);
        j++;
      }
      
      y = sub.y-20;
      setTile(result, i, y, Tile.PATHBLANK, sub.x, sub.y, midX);
      j = 1;
      while (getTileState(result, i, y-j, sub.x, sub.y, midX) == Tile.PATH &&
             getTileState(result, i-1, y-j, sub.x, sub.y, midX) == Tile.WALL &&
             getTileState(result, i+1, y-j, sub.x, sub.y, midX) == Tile.WALL) {
        setTile(result, i, y-j, Tile.PATHBLANK, sub.x, sub.y, midX);
        j++;
      }
    }
    
    for (int i =0; i < 7; i++) {
      int x = 6;
      int y = sub.y - 14 - i;
      setTile(result, x, y, Tile.PATHBLANK, sub.x, sub.y, midX);
      int j = 1;
      while (getTileState(result, x+j, y, sub.x, sub.y, midX) == Tile.PATH &&
             getTileState(result, x+j, y-1, sub.x, sub.y, midX) == Tile.WALL &&
             getTileState(result, x+j, y+1, sub.x, sub.y, midX) == Tile.WALL) {
        setTile(result, x+j, y, Tile.PATHBLANK, sub.x, sub.y, midX);
        j++;
      }
    }
     
    tileMap = result;
    
    return result;
  }
  
  
  /*** PRIVATE ***/

  private void reset() {
    Cell.numFilled = 0;
    cellMap = new Cell[cellMapSize.x][cellMapSize.y];
    // tileMap = new TileState[tileMapSize.y][tileMapSize.x];

    for (int i = 0; i != cellMapSize.x; i++) {
      for (int j = 0; j != cellMapSize.y; j++) {
        cellMap[i][j] = new Cell(i, j);
      }
    }
    for (int j = 0; j != cellMapSize.y; j++) {
      for (int i = 0; i != cellMapSize.x; i++) {
        cellMap[i][j].next[Cell.LEFT] = i > 0 ? cellMap[i - 1][j]
            : null;
        cellMap[i][j].next[Cell.RIGHT] = i < cellMapSize.x - 1 ? cellMap[i + 1][j]
            : null;
        cellMap[i][j].next[Cell.UP] = j > 0 ? cellMap[i][j - 1] : null;
        cellMap[i][j].next[Cell.DOWN] = j < cellMapSize.y - 1 ? cellMap[i][j + 1]
            : null;
      }
    }

    cellMap[0][3].isGhostSpace = true;
    cellMap[0][3].filled = true;
    cellMap[0][3].connect[Cell.LEFT] = cellMap[0][3].connect[Cell.RIGHT] = cellMap[0][3].connect[Cell.DOWN] = true;

    cellMap[1][3].isGhostSpace = true;
    cellMap[1][3].filled = true;
    cellMap[1][3].connect[Cell.LEFT] = cellMap[1][3].connect[Cell.DOWN] = true;

    cellMap[0][4].isGhostSpace = true;
    cellMap[0][4].filled = true;
    cellMap[0][4].connect[Cell.LEFT] = cellMap[0][4].connect[Cell.RIGHT] = cellMap[0][4].connect[Cell.UP] = true;

    cellMap[1][4].isGhostSpace = true;
    cellMap[1][4].filled = true;
    cellMap[1][4].connect[Cell.LEFT] = cellMap[1][4].connect[Cell.UP] = true;

    tallRows = new int[cellMapSize.x];
    for (int i = 0; i != tallRows.length; i++) {
      tallRows[i] = -1;
    }

    narrowCols = new int[cellMapSize.y];
    for (int i = 0; i != narrowCols.length; i++) {
      narrowCols[i] = -1;
    }
  }

  private ArrayList<Cell> getLeftMostEmptyCells() {
    ArrayList<Cell> result = new ArrayList<Cell>();
    for (int i = 0; i < cellMapSize.x; i++) {
      for (int j = 0; j < cellMapSize.y; j++) {
        Cell c = cellMap[i][j];
        if (!c.filled) {
          result.add(c);
        }
      }
      if (result.size() > 0) {
        break;
      }
    }
    return result;
  }

  private void attemptGenerate() {
    
    Cell cell = null;
    Cell newCell = null;
    Cell firstCell = null;
    
    int singleCount[] = new int[] { 0, 0 }; // Single cell count for
                        // position 0 and
                        // cellMapSize.y-1

    float probStopGrowingAtSize[] = new float[] { 0.0f, 0.0f, 0.1f, 0.5f,
        0.75f, 1.0f };
    float probTopAndBotSingleCellJoin = 0.35f;
    float probExtendAtSize2 = 1;
    float probExtendAtSize3or4 = 0.5f;

    int longPieces = 0;
    final int MAX_LONG_PIECES = 1;

    int dir = -1;

    
    
    for (int numGroups = 0;; numGroups++) {
      // System.out.println("\tWith numgroup "+numGroups);
      ArrayList<Cell> openCells = getLeftMostEmptyCells();

      int numOpenCells = openCells.size();
      // System.out.println("\tOpen cells: "+numOpenCells);
      if (numOpenCells == 0) {
        break;
      }

      firstCell = cell = openCells.get((int) Math.floor(r
          .nextFloat() * ((float) (numOpenCells))));
      cell.fill(numGroups);

      if (cell.position.x < cellMapSize.x - 1
          && (cell.position.y == 0 || cell.position.y == cellMapSize.y - 1)
          && r.nextFloat() < probTopAndBotSingleCellJoin) {
        int singleCountPos = cell.position.y == 0 ? 0 : 1;
        if (singleCount[singleCountPos] == 0) {
          cell.connect[cell.position.y == 0 ? Cell.UP : Cell.DOWN] = true;
          singleCount[singleCountPos]++;
          continue;
        }
      }

      int size = 1;

      if (cell.position.x == cellMapSize.x - 1) {
        cell.connect[Cell.RIGHT] = true;
        cell.isRaiseHeightCandidate = true;
      } else {
        while (size < 5) {
          boolean stop = false;

          if (size == 2) {
            Cell c = firstCell;
            if (c.position.x > 0 && c.connect[Cell.RIGHT]
                && c.next[Cell.RIGHT] != null
                && c.next[Cell.RIGHT].next[Cell.RIGHT] != null) {
              if (longPieces < MAX_LONG_PIECES
                  && r.nextFloat() < probExtendAtSize2) {
                int chosenDir = -1;
                c = c.next[Cell.RIGHT].next[Cell.RIGHT];
                boolean dirs[] = new boolean[] { false, false,
                    false, false };
                if (c.isOpen(Cell.UP)) {
                  dirs[Cell.UP] = true;
                }
                if (c.isOpen(Cell.DOWN)) {
                  dirs[Cell.DOWN] = true;
                }

                if (dirs[Cell.UP] && dirs[Cell.DOWN]) {
                  chosenDir = r.nextFloat() < 0.5 ? Cell.UP
                      : Cell.DOWN;
                } else if (dirs[Cell.UP]) {
                  chosenDir = Cell.UP;
                } else if (dirs[Cell.DOWN]) {
                  chosenDir = Cell.DOWN;
                } else {
                  chosenDir = -1;
                }

                if (chosenDir != -1) {
                  c.connect(Cell.LEFT);
                  c.fill(numGroups);
                  c.connect(chosenDir);
                  c.next[chosenDir].fill(numGroups);
                  longPieces++;
                  size += 2;
                  stop = true;
                }
              }
            }
          }

          if (!stop) {
            ArrayList<Integer> leOpenCells = cell.getOpenCells(dir,
                size);
            int numLeOpenCells = leOpenCells.size();

            if (numLeOpenCells == 0 && size == 2) {
              cell = newCell;
              leOpenCells = cell.getOpenCells(dir, size);
              numLeOpenCells = leOpenCells.size();
            }

            if (numLeOpenCells == 0) {
              stop = true;
            } else {
              dir = leOpenCells.get(
                  (int) Math.floor(r.nextFloat()
                      * (numLeOpenCells))).intValue();
              newCell = cell.next[dir];

              cell.connect(dir);

              newCell.fill(numGroups);

              size++;

              if (firstCell.position.x == 0 && size == 3) {
                stop = true;
              }

              if (r.nextFloat() < probStopGrowingAtSize[size]) {
                stop = true;
              }
            }
          }

          if (stop) {

            if (size == 1) {

            } else if (size == 2) {
              Cell c = firstCell;
              if (c.position.x == cellMapSize.x - 1) {
                if (c.connect[Cell.UP]) {
                  c = c.next[Cell.UP];
                }
                c.connect[Cell.RIGHT] = c.next[Cell.DOWN].connect[Cell.RIGHT] = true;
              }
            } else if (size == 3 || size == 4) {
              if (longPieces < MAX_LONG_PIECES
                  && firstCell.position.x > 0
                  && r.nextFloat() <= probExtendAtSize3or4) {
                ArrayList<Integer> dirs = new ArrayList<Integer>();
                for (int i = 0; i < 4; i++) {
                  if (cell.connect[i] && cell.next[i] != null
                      && cell.next[i].isOpen(i)) {
                    dirs.add(new Integer(i));
                  }
                }
                if (dirs.size() > 0) {
                  int d = dirs.get(
                      (int) Math.floor(r.nextFloat()
                          * (dirs.size())))
                      .intValue();
                  Cell c = cell.next[d];
                  c.connect(d);
                  c.next[d].fill(numGroups);
                  longPieces++;
                }
              }
            }

            break;
          }
        } // end while
      }
      //drawCellMap(getCellMap());
      //translate((1*(cellLength*cellMapSize.x+20)), 0);
    }

    setResizeCandidates();
  }

  private void setResizeCandidates() {
    for (int j = 0; j < cellMapSize.y; j++) {
      for (int i = 0; i < cellMapSize.x; i++) {
        Cell c = cellMap[i][j];
        boolean[] q = c.connect;

        if ((c.position.x == 0 || !q[Cell.LEFT])
            && (c.position.x == cellMapSize.x - 1 || !q[Cell.RIGHT])
            && q[Cell.UP] != q[Cell.DOWN]) {
          c.isRaiseHeightCandidate = true;
        }

        Cell c2 = c.next[Cell.RIGHT];
        if (c2 != null) {
          boolean[] q2 = c2.connect;
          if (((c.position.x == 0 || !q[Cell.LEFT]) && !q[Cell.UP] && !q[Cell.DOWN])
              && ((c2.position.x == cellMapSize.x - 1 || !q2[Cell.RIGHT])
                  && !q2[Cell.UP] && !q2[Cell.DOWN])) {
            c.isRaiseHeightCandidate = true;
            c2.isRaiseHeightCandidate = true;
          }
        }

        if (c.position.x == cellMapSize.x - 1 && q[Cell.RIGHT]) {
          c.isShrinkWidthCandidate = true;
        }

        if ((c.position.y == 0 || !q[Cell.UP])
            && (c.position.y == cellMapSize.y - 1 || !q[Cell.DOWN])
            && q[Cell.LEFT] != q[Cell.RIGHT]) {
          c.isShrinkWidthCandidate = true;
        }
      }
    }
  }

  private boolean isDesirable() {
    Cell c = cellMap[cellMapSize.x - 1][0];

    if (c.connect[Cell.UP] || c.connect[Cell.RIGHT]) {
      //System.out.println("Not solid top right corner");
      return false;
    }

    c = cellMap[cellMapSize.x - 1][cellMapSize.y - 1];
    if (c.connect[Cell.DOWN] || c.connect[Cell.RIGHT]) {
      //System.out.println("No solid bottom right corner");
      return false;
    }

    for (int y = 0; y < cellMapSize.y - 1; y++) {
      for (int x = 0; x < cellMapSize.x - 1; x++) {
        if (isStackedHorizontal(x, y) && isStackedHorizontal(x, y + 1)
            || isStackedVertical(x, y)
            && isStackedVertical(x + 1, y)) {
          if (x == 0) {
            //System.out
            //    .println("Not double stacked pieces in the middle");
            return false;
          }

          cellMap[x][y].connect[Cell.DOWN] = true;
          cellMap[x][y].connect[Cell.RIGHT] = true;
          int g = cellMap[x][y].group;

          cellMap[x + 1][y].connect[Cell.DOWN] = true;
          cellMap[x + 1][y].connect[Cell.LEFT] = true;
          cellMap[x + 1][y].group = g;

          cellMap[x][y + 1].connect[Cell.UP] = true;
          cellMap[x][y + 1].connect[Cell.RIGHT] = true;
          cellMap[x][y + 1].group = g;

          cellMap[x + 1][y + 1].connect[Cell.UP] = true;
          cellMap[x + 1][y + 1].connect[Cell.LEFT] = true;
          cellMap[x + 1][y + 1].group = g;
        }
      }
    }

    if (!chooseTallRows()) {
      //System.out.println("Couldn't choose tall rows");
      return false;
    }

    if (!chooseNarrowCols()) {
      //System.out.println("Couldn't choose narrow cols");
      return false;
    }

    return true;
  }

  private void setUpScaleCoords() {
    for (int j = 0; j < cellMapSize.y; j++) {
      for (int i = 0; i < cellMapSize.x; i++) {
        Cell c = cellMap[i][j];

        c.tilePosition.x = c.position.x * 3;
        if (narrowCols[c.position.y] < c.position.x) {
          c.tilePosition.x--;
        }

        c.tilePosition.y = c.position.y * 3;
        if (tallRows[c.position.x] < c.position.y) {
          c.tilePosition.y++;
        }
        c.tileSize.x = c.shrinkWidth ? 2 : 3;
        c.tileSize.y = c.raiseHeight ? 4 : 3;
      }
    }
  }

  private void joinWalls() {
    for (int x = 0; x < cellMapSize.x; x++) {
      Cell c = cellMap[x][0];
      if (!c.connect[Cell.LEFT]
          && !c.connect[Cell.RIGHT]
          && !c.connect[Cell.UP]
          && (!c.connect[Cell.DOWN] || !c.next[Cell.DOWN].connect[Cell.DOWN])) {

        if ((c.next[Cell.LEFT] == null || !c.next[Cell.LEFT].connect[Cell.UP])
            && (c.next[Cell.RIGHT] != null && !c.next[Cell.RIGHT].connect[Cell.UP])) {

          if (!(c.next[Cell.DOWN] != null
              && c.next[Cell.DOWN].connect[Cell.RIGHT] && c.next[Cell.DOWN].next[Cell.RIGHT].connect[Cell.RIGHT])) {
            c.isJoinCandidate = true;
            if (r.nextFloat() <= 0.25) {
              c.connect[Cell.UP] = true;
            }
          }
        }
      }
    }

    for (int x = 0; x < cellMapSize.x; x++) {
      Cell c = cellMap[x][cellMapSize.y - 1];
      if (!c.connect[Cell.LEFT]
          && !c.connect[Cell.RIGHT]
          && !c.connect[Cell.DOWN]
          && (!c.connect[Cell.UP] || !c.next[Cell.UP].connect[Cell.UP])) {

        if ((c.next[Cell.LEFT] == null || !c.next[Cell.LEFT].connect[Cell.DOWN])
            && (c.next[Cell.RIGHT] != null && !c.next[Cell.RIGHT].connect[Cell.DOWN])) {

          if (!(c.next[Cell.UP] != null
              && c.next[Cell.UP].connect[Cell.RIGHT] && c.next[Cell.UP].next[Cell.RIGHT].connect[Cell.RIGHT])) {
            c.isJoinCandidate = true;
            if (r.nextFloat() <= 0.25) {
              c.connect[Cell.DOWN] = true;
            }
          }
        }
      }
    }

    for (int y = 1; y < cellMapSize.y - 1; y++) {
      Cell c = cellMap[cellMapSize.x - 1][y];

      if (c.raiseHeight) {
        continue;
      }

      if (!c.connect[Cell.RIGHT] && !c.connect[Cell.UP]
          && !c.connect[Cell.DOWN]
          && !c.next[Cell.UP].connect[Cell.RIGHT]
          && !c.next[Cell.DOWN].connect[Cell.RIGHT]) {

        if (c.connect[Cell.LEFT]) {
          Cell c2 = c.next[Cell.LEFT];
          if (!c2.connect[Cell.UP] && !c2.connect[Cell.DOWN]
              && !c2.connect[Cell.LEFT]) {
            c.isJoinCandidate = true;
            if (r.nextFloat() <= 0.5) {
              c.connect[Cell.RIGHT] = true;
            }
          }
        }
      }
    }
  }

  private boolean createTunnels() {
    ArrayList<Cell> singleDeadEndCells = new ArrayList<Cell>();
    ArrayList<Cell> topSingleDeadEndCells = new ArrayList<Cell>();
    ArrayList<Cell> botSingleDeadEndCells = new ArrayList<Cell>();

    ArrayList<Cell> voidTunnelCells = new ArrayList<Cell>();
    ArrayList<Cell> topVoidTunnelCells = new ArrayList<Cell>();
    ArrayList<Cell> botVoidTunnelCells = new ArrayList<Cell>();

    ArrayList<Cell> edgeTunnelCells = new ArrayList<Cell>();
    ArrayList<Cell> topEdgeTunnelCells = new ArrayList<Cell>();
    ArrayList<Cell> botEdgeTunnelCells = new ArrayList<Cell>();

    ArrayList<Cell> doubleDeadEndCells = new ArrayList<Cell>();

    int numTunnelsCreated = 0;

    boolean upDead = false;
    boolean downDead = false;
    for (int y = 0; y < cellMapSize.y; y++) {
      Cell c = cellMap[cellMapSize.x - 1][y];
      if (c.connect[Cell.UP]) {
        continue;
      }
      if (c.position.y > 1 && c.position.y < cellMapSize.y - 2) {
        c.isEdgeTunnelCandidate = true;
        edgeTunnelCells.add(c);
        if (c.position.y <= 2) {
          topEdgeTunnelCells.add(c);
        } else if (c.position.y >= cellMapSize.y - 4) {
          botEdgeTunnelCells.add(c);
        }
      }
      upDead = (c.next[Cell.UP] == null || c.next[Cell.UP].connect[Cell.RIGHT]);
      downDead = (c.next[Cell.DOWN] == null || c.next[Cell.DOWN].connect[Cell.RIGHT]);
      if (c.connect[Cell.RIGHT]) {
        if (upDead) {
          c.isVoidTunnelCandidate = true;
          voidTunnelCells.add(c);
          if (c.position.y <= 2) {
            topVoidTunnelCells.add(c);
          } else if (c.position.y >= cellMapSize.y - 3) {
            botVoidTunnelCells.add(c);
          }
        }
      } else {
        if (c.connect[Cell.DOWN]) {
          continue;
        }
        if (upDead != downDead) {
          if (!c.raiseHeight && y < cellMapSize.y - 1
              && !c.next[Cell.LEFT].connect[Cell.LEFT]) {
            singleDeadEndCells.add(c);
            c.isSingleDeadEndCandidate = true;
            c.singleDeadEndDir = upDead ? Cell.UP : Cell.DOWN;
            int offset = upDead ? 1 : 0;
            if (c.position.y <= 1 + offset) {
              topSingleDeadEndCells.add(c);
            } else if (c.position.y >= cellMapSize.y - 4 + offset) {
              botSingleDeadEndCells.add(c);
            }
          }
        } else if (upDead && downDead) {
          if (y > 0 && y < cellMapSize.y - 1) {
            if (c.next[Cell.LEFT].connect[Cell.UP]
                && c.next[Cell.LEFT].connect[Cell.DOWN]) {
              c.isDoubleDeadEndCandidate = true;
              if (c.position.y >= 2
                  && c.position.y <= cellMapSize.y - 4) {
                doubleDeadEndCells.add(c);
              }
            }
          }
        }
      }
    }

    int numTunnelsDesired = r.nextFloat() <= 0.45 ? 2 : 1;

    if (numTunnelsDesired == 1) {
      if (voidTunnelCells.size() > 0) {
        voidTunnelCells.get((int) Math.floor(r.nextFloat()
            * (voidTunnelCells.size()))).topTunnel = true;
      } else if (singleDeadEndCells.size() > 0) {
        selectSingleDeadEnd(singleDeadEndCells.get((int) Math.floor(r
            .nextFloat() * (singleDeadEndCells.size()))));
      } else if (edgeTunnelCells.size() > 0) {
        edgeTunnelCells.get((int) Math.floor(r.nextFloat()
            * (edgeTunnelCells.size()))).topTunnel = true;
      } else {
        return false;
      }
    } else if (numTunnelsDesired == 2) {
      if (doubleDeadEndCells.size() > 0) {
        Cell c = doubleDeadEndCells.get((int) Math.floor(r.nextFloat()
            * (doubleDeadEndCells.size())));
        c.connect[Cell.RIGHT] = true;
        c.topTunnel = true;
        c.next[Cell.DOWN].topTunnel = true;
      } else {
        numTunnelsCreated = 1;
        if (topVoidTunnelCells.size() > 0) {
          topVoidTunnelCells.get((int) Math.floor(r.nextFloat()
              * (topVoidTunnelCells.size()))).topTunnel = true;
        } else if (topSingleDeadEndCells.size() > 0) {
          selectSingleDeadEnd(topSingleDeadEndCells.get((int) Math
              .floor(r.nextFloat()
                  * (topSingleDeadEndCells.size()))));
        } else if (topEdgeTunnelCells.size() > 0) {
          topEdgeTunnelCells.get((int) Math.floor(r.nextFloat()
              * (topEdgeTunnelCells.size()))).topTunnel = true;
        } else {
          numTunnelsCreated = 0;
        }

        if (botVoidTunnelCells.size() > 0) {
          botVoidTunnelCells.get((int) Math.floor(r.nextFloat()
              * (botVoidTunnelCells.size()))).topTunnel = true;
        } else if (botSingleDeadEndCells.size() > 0) {
          selectSingleDeadEnd(botSingleDeadEndCells.get((int) Math
              .floor(r.nextFloat()
                  * (botSingleDeadEndCells.size()))));
        } else if (botEdgeTunnelCells.size() > 0) {
          botEdgeTunnelCells.get((int) Math.floor(r.nextFloat()
              * (botEdgeTunnelCells.size()))).topTunnel = true;
        } else {
          if (numTunnelsCreated == 0) {
            return false;
          }
        }
      }
    }

    boolean exit = false;
    int topy = -1;
    for (int y = 0; y < cellMapSize.y; y++) {
      Cell c = cellMap[cellMapSize.x - 1][y];
      if (c.topTunnel) {
        exit = true;
        topy = c.tilePosition.y;
        while (c.next[Cell.LEFT] != null) {
          c = c.next[Cell.LEFT];
          if (!c.connect[Cell.UP] && c.tilePosition.y == topy) {
            continue;
          } else {
            exit = false;
            break;
          }
        }
        if (exit) {
          return false;
        }
      }
    }

    int len = voidTunnelCells.size();

    for (int i = 0; i < len; i++) {
      Cell c = voidTunnelCells.get(i);
      if (!c.topTunnel) {
        replaceGroup(c.group, c.next[Cell.UP].group);
        c.connect[Cell.UP] = true;
        c.next[Cell.UP].connect[Cell.DOWN] = true;
      }
    }

    return true;
  }

  /*** GENERATE HELPER FUNCTIONS ***/

  private boolean isStackedHorizontal(int x, int y) {
    boolean q1[] = cellMap[x][y].connect;
    boolean q2[] = cellMap[x + 1][y].connect;
    return !q1[Cell.UP] && !q1[Cell.DOWN] && (x == 0 || !q1[Cell.LEFT])
        && q1[Cell.RIGHT] && !q2[Cell.UP] && !q2[Cell.DOWN]
        && q2[Cell.LEFT] && !q2[Cell.RIGHT];
  }

  private boolean isStackedVertical(int x, int y) {
    boolean q1[] = cellMap[x][y].connect;
    boolean q2[] = cellMap[x][y + 1].connect;

    if (x == cellMapSize.x - 1) {
      return !q1[Cell.LEFT] && !q1[Cell.UP] && !q1[Cell.DOWN]
          && !q2[Cell.LEFT] && !q2[Cell.UP] && !q2[Cell.DOWN];
    }
    return !q1[Cell.LEFT] && !q1[Cell.RIGHT] && !q1[Cell.UP]
        && q1[Cell.DOWN] && !q2[Cell.LEFT] && !q2[Cell.RIGHT]
        && q2[Cell.UP] && !q2[Cell.DOWN];
  }

  private boolean chooseTallRows() {
    for (int y = 0; y < 3; y++) {
      Cell c = cellMap[0][y];
      if (c.isRaiseHeightCandidate && canRaiseHeight(0, y)) {
        c.raiseHeight = true;
        tallRows[c.position.x] = c.position.y;
        return true;
      }
    }

    return false;
  }

  private boolean canRaiseHeight(int x, int y) {
    if (x == cellMapSize.x - 1) {
      return true;
    }
    Cell c2 = null;

    for (int y0 = y; y0 >= 0; y0--) {
      Cell c = cellMap[x][y0];
      c2 = c.next[Cell.RIGHT];
      if ((!c.connect[Cell.UP] || c.isCrossCenter())
          && (!c2.connect[Cell.UP] || c2.isCrossCenter())) {
        break;
      }
    }

    ArrayList<Cell> candidates = new ArrayList<Cell>();
    for (; c2 != null; c2 = c2.next[Cell.DOWN]) {
      if (c2.isRaiseHeightCandidate) {
        candidates.add(c2);
      }

      if ((!c2.connect[Cell.DOWN] || c2.isCrossCenter())
          && (!c2.next[Cell.LEFT].connect[Cell.DOWN] || c2.next[Cell.LEFT]
              .isCrossCenter())) {
        break;
      }
    }
    Collections.shuffle(candidates);

    for (int i = 0; i < candidates.size(); i++) {
      c2 = candidates.get(i);
      if (canRaiseHeight(c2.position.x, c2.position.y)) {
        c2.raiseHeight = true;
        tallRows[c2.position.x] = c2.position.y;
        return true;
      }
    }

    return false;
  }

  private boolean chooseNarrowCols() {
    for (int x = cellMapSize.x - 1; x >= 0; x--) {
      Cell c = cellMap[x][0];
      if (c.isShrinkWidthCandidate && canShrinkWidth(x, 0)) {
        c.shrinkWidth = true;
        narrowCols[c.position.y] = c.position.x;
        return true;
      }
    }

    return false;
  }

  private boolean canShrinkWidth(int x, int y) {
    if (y == cellMapSize.y - 1) {
      return true;
    }

    Cell c2 = null;
    for (int x0 = x; x0 < cellMapSize.x; x0++) {
      Cell c = cellMap[x0][y];
      c2 = c.next[Cell.DOWN];
      if ((!c.connect[Cell.RIGHT] || c.isCrossCenter())
          && (!c2.connect[Cell.RIGHT] || c.isCrossCenter())) {
        break;
      }
    }

    ArrayList<Cell> candidates = new ArrayList<Cell>();
    for (; c2 != null; c2 = c2.next[Cell.LEFT]) {
      if (c2.isShrinkWidthCandidate) {
        candidates.add(c2);
      }

      if ((!c2.connect[Cell.LEFT] || c2.isCrossCenter())
          && (!c2.next[Cell.UP].connect[Cell.LEFT] || c2.next[Cell.UP]
              .isCrossCenter())) {
        break;
      }
    }
    Collections.shuffle(candidates);

    for (int i = 0; i != candidates.size(); i++) {
      c2 = candidates.get(i);
      if (canShrinkWidth(c2.position.x, c2.position.y)) {
        c2.shrinkWidth = true;
        narrowCols[c2.position.y] = c2.position.x;
        return true;
      }
    }

    return false;
  }

  private void selectSingleDeadEnd(Cell c) {
    c.connect[Cell.RIGHT] = true;
    if (c.singleDeadEndDir == Cell.UP) {
      c.topTunnel = true;
    } else {
      c.next[Cell.DOWN].topTunnel = true;
    }
  }

  private void replaceGroup(int oldGroup, int newGroup) {
    for (int j = 0; j != cellMapSize.y; j++) {
      for (int i = 0; i != cellMapSize.x; i++) {
        Cell c = cellMap[i][j];
        if (c.group == oldGroup) {
          c.group = newGroup;
        }
      }
    }
  }
  
  /*** TILE CONSTRUCTION FUNCTIONS ***/
  
  
  private void setTile(Tile[][] tM, int x, int y, int state, int w, int h, int midX) {
    if (x < 0 || x >= w || y < 0 || y >= h) {
      return;
    }
    x -= 2;
    tM[midX+x][y].state = state;
    tM[midX-1-x][y].state = state;
  }
  
  int getTileState(Tile[][] tM, int x, int y, int w, int h, int midX) {
    if (x < 0 || x >= w || y < 0 || y >= h) {
      return -1;
    }
    x -= 2;
    return tM[midX+x][y].state;
  }
  
  private float[] getTopEnergizerRange(Tile[][] tM, int w, int h, int midX) {
    int x = w-2;
    int miny = 0;
    int maxy = h/2;
    for (int y = 1; y < maxy; y++) {
      if (getTileState(tM, x, y, w, h, midX) == Tile.PATH &&
          getTileState(tM, x, y+1, w, h, midX) == Tile.PATH) {
        miny = y+1;
        break;
      }
    }
    maxy = Math.min(maxy, miny+7);
    for (int y = miny+1; y < maxy; y++) {
      if (getTileState(tM, x-1, y, w, h, midX) == Tile.PATH) {
        maxy = y;
        break;
      }
    }
    
    return new float[]{miny, maxy-1};
  }
  
  private float[] getBotEnergizerRange(Tile[][] tM, int w, int h, int midX) {
    int x = w-2;
    int miny = h/2;
    int maxy = 0;
    for (int y = h-3; y >= miny; y--) {
      if (getTileState(tM, x, y, w, h, midX) == Tile.PATH &&
          getTileState(tM, x, y+1, w, h, midX) == Tile.PATH) {
        maxy = y;
        break;
      }
    }
    miny = Math.max(miny, maxy-7);
    for (int y = maxy-1; y > miny; y--) {
      if (getTileState(tM, x-1, y, w, h, midX) == Tile.PATH) {
        miny = y+1;
        break;
      }
    }
    
    return new float[]{miny, maxy-1};
  }
  
  private void eraseUntilIntersection(Tile[][] tM, int x, int y, int w, int h, int midX) {
    while (true) {
      ArrayList<IntTuple> adj = new ArrayList<IntTuple>();
      if (getTileState(tM, x-1, y, w, h, midX) == Tile.PATH) {
        adj.add(new IntTuple(x-1, y));
      }
      if (getTileState(tM, x+1, y, w, h, midX) == Tile.PATH) {
        adj.add(new IntTuple(x+1, y));
      }
      if (getTileState(tM, x, y-1, w, h, midX) == Tile.PATH) {
        adj.add(new IntTuple(x, y-1));
      }
      if (getTileState(tM, x, y+1, w, h, midX) == Tile.PATH) {
        adj.add(new IntTuple(x, y+1));
      }
      if (adj.size() == 1) {
        setTile(tM, x, y, Tile.PATHBLANK, w, h, midX);
        x = adj.get(0).x;
        y = adj.get(0).y;
      } else {
        break;
      }
    }
  }
}
