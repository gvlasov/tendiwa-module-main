package tendiwa.modules;

import tendiwa.core.*;
import tendiwa.resources.WallTypes;

import java.util.Set;

public class Forest implements LocationDrawer {

@Override
public void draw(Location location, LocationPlace place) {
	int width = place.width;
	int height = place.height;
	location.line(1, 1, width - 3, height - 3, WallTypes.wall_grey_stone);
	location.line(width - 3, 1, 1, height - 3, WallTypes.wall_grey_stone);
}

@Override
public boolean meetsRequirements(Set<LocationFeature> features) {
	return false;
}

@Override
public boolean canHandlePaths(Set<PathSegment> paths) {
	return paths.size() <= 2;
//		&& paths.stream().allMatch(ps -> ps.getType() == Road.class);
}

}
