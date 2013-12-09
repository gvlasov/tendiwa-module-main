package tendiwa.modules;

import org.tendiwa.entities.Characters;
import org.tendiwa.entities.Items;
import tendiwa.core.Character;
import tendiwa.core.*;
import tendiwa.drawing.*;

import java.awt.*;

import static tendiwa.core.DSL.canvas;

public class MainModule extends Module implements WorldProvider {

public MainModule() {
	DefaultDrawingAlgorithms.register(EnhancedRectangle.class, DrawingRectangle.withColorLoop(Color.GRAY, Color.BLACK, Color.BLUE));
	DefaultDrawingAlgorithms.register(RectangleSystem.class, DrawingRectangleSystem
		.withColors(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW));
	DefaultDrawingAlgorithms.register(
		RectangleSidePiece.class,
		DrawingRectangleSidePiece.withColor(Color.MAGENTA));
	DefaultDrawingAlgorithms.register(Segment.class, DrawingSegment.withColor(Color.BLUE));
	DefaultDrawingAlgorithms.register(Chunk.class, DrawingTerrain.defaultAlgorithm());
	DefaultDrawingAlgorithms.register(World.class, DrawingWorld.defaultAlgorithm());

	ResourcesRegistry.registerDrawer(new TestLocationDrawer());
//	ResourcesRegistry.registerDrawer(new Forest());
	ResourcesRegistry.registerDrawer(new Ocean());
}

public static void main(String[] args) {
	MainModule mainModule = new MainModule();
	TestCanvas canvas = canvas();
	canvas.draw(mainModule.createWorld());
}

@Override
public World createWorld() {
	World world = World.create(new SuseikaWorld(), 400, 300);
	Character playerCharacter = world.createCharacter(120, 130, Characters.human, "Suseika");
	world.setPlayerCharacter(playerCharacter);
	playerCharacter.getItem(Items.shortBow);
	playerCharacter.getItem(Items.shortBow);
	playerCharacter.getItem(Items.shortBow);
	playerCharacter.getItem(Items.shortBow);
	playerCharacter.getItem(Items.shortBow);
	playerCharacter.getItem(Items.shortBow);
	playerCharacter.getItem(Items.shortBow);
	playerCharacter.getItem(Items.ironArmor);
	playerCharacter.getItem(Items.woodenArrow, 100);
	playerCharacter.getItem(Items.ironHelm);
	return world;
}
}
