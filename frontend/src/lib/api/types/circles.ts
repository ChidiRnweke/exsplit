import type { components as CirclesComponents } from '../schemas/exsplit.spec.CirclesService';

export type CreateCircleInput = CirclesComponents['schemas']['CreateCircleRequestContent'];
export type CreateCircleOutput = CirclesComponents['schemas']['CreateCircleResponseContent'];
export type UserForCircleOutput = CirclesComponents['schemas']['ListCirclesForUserResponseContent'];
export type UpdateCircleInput = CirclesComponents['schemas']['UpdateCircleRequestContent'];
export type GetCircleOutput = CirclesComponents['schemas']['GetCircleResponseContent'];
export type ListCircleMembersOutput =
	CirclesComponents['schemas']['ListCircleMembersResponseContent'];
export type AddCircleMemberInput = CirclesComponents['schemas']['AddUserToCircleRequestContent'];
export type CircleId = { circleId: string };
export type MemberId = { memberId: string };
