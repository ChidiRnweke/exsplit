import createClient from 'openapi-fetch';
import { throwIfError } from './util/ErrorHandling';
import type {
	CreateCircleInput,
	CreateCircleOutput,
	CircleId,
	UpdateCircleInput,
	GetCircleOutput,
	ListCircleMembersOutput,
	AddCircleMemberInput,
	MemberId,
	UserForCircleOutput
} from '../types/circles';
import type { UserId } from '../types/user';
import type { paths as CirclesPaths } from '../schemas/exsplit.spec.CirclesService';
import { authMiddleware } from './util/Auth';

export interface CirclesService {
	createCircle: (userId: UserId, circle: CreateCircleInput) => Promise<CreateCircleOutput>;
	updateCircle: (circleId: CircleId, data: UpdateCircleInput) => Promise<void>;
	deleteCircle: (circleId: CircleId) => Promise<void>;
	getCircle: (circleId: CircleId) => Promise<GetCircleOutput>;
	listCircleMembers: (circleId: CircleId) => Promise<ListCircleMembersOutput>;
	addCircleMember: (circleId: CircleId, member: AddCircleMemberInput) => Promise<void>;
	removeCircleMember: (circleId: CircleId, memberId: MemberId) => Promise<void>;
	listCirclesForUser: (userId: UserId) => Promise<UserForCircleOutput>;
	changeMemberDisplayName: (
		circleId: CircleId,
		memberId: MemberId,
		displayName: string
	) => Promise<void>;
}

class CirclesClient implements CirclesService {
	private client = createClient<CirclesPaths>({ baseUrl: '/' });

	constructor() {
		this.client.use(authMiddleware);
	}

	createCircle = async (userId: UserId, circle: CreateCircleInput): Promise<CreateCircleOutput> => {
		const { data, error } = await this.client.POST('/api/users/{userId}/circles', {
			params: { path: userId },
			body: circle
		});
		return throwIfError(data, error);
	};
	updateCircle = async (circleId: CircleId, data: UpdateCircleInput): Promise<void> => {
		const { error } = await this.client.PATCH('/api/circles/{circleId}', {
			params: { path: circleId },
			body: data
		});
		throwIfError(undefined, error);
	};
	deleteCircle = async (circleId: CircleId): Promise<void> => {
		const { error } = await this.client.DELETE('/api/circles/{circleId}', {
			params: { path: circleId }
		});
		throwIfError(undefined, error);
	};
	getCircle = async (circleId: CircleId): Promise<GetCircleOutput> => {
		const { data, error } = await this.client.GET('/api/circles/{circleId}', {
			params: { path: circleId }
		});
		return throwIfError(data, error);
	};
	listCircleMembers = async (circleId: CircleId): Promise<ListCircleMembersOutput> => {
		const { data, error } = await this.client.GET('/api/circles/{circleId}/members', {
			params: { path: circleId }
		});
		return throwIfError(data, error);
	};
	addCircleMember = async (circleId: CircleId, member: AddCircleMemberInput): Promise<void> => {
		const { error } = await this.client.POST('/api/circles/{circleId}/members', {
			params: { path: circleId },
			body: member
		});
		throwIfError(undefined, error);
	};
	removeCircleMember = async (circleId: CircleId, memberId: MemberId): Promise<void> => {
		const { error } = await this.client.DELETE('/api/circles/{circleId}/members/{memberId}', {
			params: { path: { ...circleId, ...memberId } }
		});
		throwIfError(undefined, error);
	};
	listCirclesForUser = async (userId: UserId): Promise<UserForCircleOutput> => {
		const { data, error } = await this.client.GET('/api/users/{userId}/circles', {
			params: { path: userId }
		});
		return throwIfError(data, error);
	};
	changeMemberDisplayName = async (
		circleId: CircleId,
		memberId: MemberId,
		displayName: string
	): Promise<void> => {
		const { error } = await this.client.PATCH('/api/circles/{circleId}/members/{memberId}', {
			params: { path: { ...circleId, ...memberId } },
			body: { displayName: displayName }
		});
		throwIfError(undefined, error);
	};
}

export const circlesClient = new CirclesClient();
