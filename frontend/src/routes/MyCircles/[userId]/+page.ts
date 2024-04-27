import { circlesClient } from '$lib/api/clients/CirclesService';
import type { UserForCircleOutput } from '$lib/api/types/circles';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ params }): Promise<UserForCircleOutput> => {
	const userId = params.userId;
	const circles = await circlesClient.listCirclesForUser({ userId });
	return circles;
};
