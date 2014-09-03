package org.tendiwa.modules.mainModule

import org.jgrapht.UndirectedGraph
import org.tendiwa.core.Location
import org.tendiwa.core.ScriptShell
import org.tendiwa.core.World
import org.tendiwa.demos.settlements.CityDrawer
import org.tendiwa.drawing.DrawingAlgorithm
import org.tendiwa.drawing.TestCanvas
import org.tendiwa.drawing.extensions.*
import org.tendiwa.geometry.*
import org.tendiwa.geometry.extensions.*
import org.tendiwa.noise.Noise
import org.tendiwa.noise.SimpleNoiseSource
import org.tendiwa.pathfinding.astar.AStar
import org.tendiwa.pathfinding.dijkstra.PathTable
import org.tendiwa.settlements.*
import org.tendiwa.settlements.utils.RectangularBuildingLots

import java.awt.Color

ScriptShell.loadGameTypesDeclarations(this);
def world = new World(400, 300);
def location = new Location(world.getDefaultPlane(), 0, 0, world.width, world.height);
location.fillRectangle(new Rectangle(100, 100, 100, 100), org.tendiwa.groovy.DSL.wallTypes.wall_grey_stone);
PieChartTimeProfiler chart = new PieChartTimeProfiler();
int maxCityRadius = 35;
int minDistanceFromCoastToCityBorder = 3;
int minDistanceBetweenCityCenters = maxCityRadius * 3;
int minDistanceFromCoastToCityCenter = 20;
SimpleNoiseSource noise = { int x, int y ->
    Noise.noise(
            ((double) x + 0) / 50,
            ((double) y + 0) / 40,
            7
    );
} as SimpleNoiseSource
Rectangle worldSize = new Rectangle(20, 20, 1000, 1000);
CellSet water = { int x, int y -> noise.noise(x, y) <= 110; } as CellSet
chart.saveTime("Constants");
CellSet reducingMask = { x, y -> (x + y) % 20 == 0; } as CellSet
ChebyshevDistanceBufferBorder cityCenterBorder = new ChebyshevDistanceBufferBorder(
        minDistanceFromCoastToCityCenter,
        { int x, int y -> worldSize.contains(x, y) && water.contains(x, y) } as CellSet
);
FiniteCellSet borderWithCityCenters = new ScatteredCellSet(
        reducingMask.and(cityCenterBorder),
        worldSize
);
chart.saveTime("City centers");
CachedCellSet cellsCloseToCoast = new CachedCellSet(
        new ChebyshevDistanceBuffer(
                minDistanceFromCoastToCityBorder,
                { x, y -> worldSize.contains(x, y) && water.contains(x, y) } as CellSet
        ),
        worldSize
);
chart.saveTime("Cells close to coast");
DistantCellsFinder cityCenters = new DistantCellsFinder(
        borderWithCityCenters,
        minDistanceBetweenCityCenters
);
chart.saveTime("Distant cells");
//    @Inject
//    @Named("scale2")
DrawingAlgorithm<Cell> grassColor = DrawingCell.withColor(Color.GREEN);
DrawingAlgorithm<Cell> waterColor = DrawingCell.withColor(Color.BLUE);

CityBoundsFactory boundsFactory = new CityBoundsFactory(water);
Rectangle worldSizeStretchedBy1 = worldSize.stretch(1);
canvas = new TestCanvas(1, worldSize.x + worldSize.getMaxX(), worldSize.y + worldSize.getMaxY());
TestCanvas.canvas = canvas;
canvas.draw(borderWithCityCenters, DrawingCellSet.withColor(Color.PINK));
drawTerrain(worldSize, water, waterColor, grassColor);
chart.saveTime("Draw terrain");
//        canvas.draw(borderWithCityCenters, DrawingCellSet.withColor(Color.RED));
Collection<FiniteCellSet> shapeExitsSets = new HashSet<>();
MutableCellSet citiesCells = new ScatteredMutableCellSet();
for (Cell cell : cityCenters) {
    chart.saveTime("0");
    int maxCityRadiusModified = maxCityRadius + cell.x % 30 - 15;
    Rectangle cityBoundRec = Recs
            .rectangleByCenterPoint(cell, maxCityRadiusModified * 2 + 1, maxCityRadiusModified * 2 + 1)
            .intersectionWith(worldSize)
            .get();
    CachedCellSet coast = new CachedCellSet(
            new ChebyshevDistanceBufferBorder(minDistanceFromCoastToCityBorder, water),
            cityBoundRec
    );
    chart.saveTime("1");
    BoundedCellSet cityShape = new PathTable(
            cell.x,
            cell.y,
            { x, y -> worldSizeStretchedBy1.contains(x, y) && !coast.contains(x, y) },
            maxCityRadiusModified
    ).computeFull();
    chart.saveTime("2");
//            canvas.draw(cell, DrawingCell.withColorAndSize(Color.black, 6));
//            canvas.draw(cityShape, DrawingCellSet.withColor(Color.BLACK));
    UndirectedGraph<Point2D, Segment2D> cityBounds = boundsFactory.create(
            cityShape,
            cell,
            maxCityRadiusModified
    );
    chart.saveTime("3");
//            canvas.draw(cityBounds, DrawingGraph.withColorAndVertexSize(RED, 2));
    RoadsPlanarGraphModel city = new CityGeometryBuilder(cityBounds)
            .withDefaults()
            .withRoadsFromPoint(4)
            .withDeviationAngle(Math.PI / 30)
            .withSecondaryRoadNetworkDeviationAngle(0.1)
            .withRoadSegmentLength(30)
            .withConnectivity(1)
            .withMaxStartPointsPerCycle(3)
            .build();
    chart.saveTime("4");
    citiesCells.addAll(ShapeFromOutline.from(city.getLowLevelRoadGraph()));
    chart.saveTime("5");
    canvas.draw(city, new CityDrawer());
    FiniteCellSet exitCells = null;
    try {
        exitCells = city
                .getNetworks()
                .stream()
                .flatMap({ c ->
            c
                    .exitsOnCycles()
                    .stream()
                    .filter({ p ->
                c
                        .network()
                        .edgeSet()
                        .stream()
                        .anyMatch({ e -> e.start.equals(p) || e.end.equals(p) })
            })
                    .map({ p -> p.toCell() })
        })
                .collect(CellSet.toCellSet());
    } catch (Exception exc) {
        TestCanvas cvs = new TestCanvas(2, worldSize.x + worldSize.getMaxX(),
                worldSize.y + worldSize.getMaxY());
        for (NetworkWithinCycle net : city.getNetworks()) {
            cvs.draw(net.cycle().asGraph(), DrawingGraph.withColorAndAntialiasing(Color.BLACK));
        }
        throw new RuntimeException();
    }
    chart.saveTime("6");
    shapeExitsSets.add(exitCells);
    chart.saveTime("7");
    for (RectangleWithNeighbors rectangleWithNeighbors : RectangularBuildingLots.placeInside(city)) {
        canvas.draw(
                rectangleWithNeighbors.rectangle,
                DrawingRectangle.withColorAndBorder(Color.blue, Color.gray)
        );
        for (Rectangle neighbor : rectangleWithNeighbors.neighbors) {
            canvas.draw(
                    neighbor,
                    DrawingRectangle.withColorAndBorder(Color.magenta, Color.magenta.darker())
            );
        }
    }
}
CellSet shapeExitsCombined = shapeExitsSets
        .stream()
        .map({ a -> (CellSet) a })
        .reduce(CellSet.empty(), { a, b -> a.or(b) });
chart.saveTime("Combined sets");


CellSet spaceBetweenCities = new CachedCellSet(
        { x, y ->
            worldSize.contains(x, y) && (!water.contains(x, y)
                    && !citiesCells.contains(x, y)
                    && !cellsCloseToCoast.contains(x, y) || shapeExitsCombined.contains(x, y))
        },
        worldSize
);
chart.saveTime("Space between cities");
IntershapeNetwork network = IntershapeNetwork
        .withShapeExits(shapeExitsSets)
        .withWalkableCells(spaceBetweenCities);
chart.saveTime("Network");
//        for (Cell cell : wave) {
//            canvas.draw(cell, DrawingCell.withColor(Color.DARK_GRAY));
//        }
//		canvas.draw(citiesCells, DrawingCellSet.withColor(Color.DARK_GRAY));
//		for (FiniteCellSet exits : shapeExitsSets) {
//			canvas.draw(exits, DrawingCellSet.withColor(Color.RED));
//		}

for (CellSegment segment : network.getGraph().edgeSet()) {
//            canvas.draw(segment, DrawingCellSegment.withColor(Color.RED));
    List<Cell> path = new AStar(
            { cell, neighbor ->
                ((spaceBetweenCities.contains(neighbor) ? 1 : 100000000) * cell.diagonalComponent(neighbor))
            }
    ).path(segment.start, segment.end);
    path.stream().forEach({ c -> canvas.draw(c, DrawingCell.withColor(Color.RED)) });
}
chart.saveTime("Final drawing");
canvas.draw(cellsCloseToCoast, DrawingCellSet.withColor(Color.PINK));
