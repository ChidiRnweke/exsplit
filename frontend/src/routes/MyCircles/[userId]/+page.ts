import { circlesClient } from '$lib/api/clients/CirclesService';
import type { Circle } from '$lib/types/circles';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }): Promise<Circle[]> => {
	const userId = params.userId;
	const circles = await circlesClient.listCirclesForUser({ userId });
	return circles.circles;
};
