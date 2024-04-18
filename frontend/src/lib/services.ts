import createClient from 'openapi-fetch';
import type {
	components as UserComponents,
	paths as UserPaths
} from './schemas/exsplit.spec.UserService';
import type {
	components as CirclesComponents,
	paths as CirclesPaths
} from './schemas/exsplit.spec.CirclesService';
import type {
	components as ExpenseComponents,
	paths as ExpensePaths
} from './schemas/exsplit.spec.ExpenseService';
import type {
	components as ExpenseListComponents,
	paths as ExpenseListPaths
} from './schemas/exsplit.spec.ExpenseListService';

type APIError = {
	message: string;
};

export const throwIfError = <T>(data: T | undefined, error: APIError | undefined): T => {
	if (!data) {
		console.error('API error:', error);

		throw new Error(error?.message || 'An unknown error occurred');
	}
	return data;
};

type LoginInput = UserComponents['schemas']['LoginRequestContent'];
type LoginOutput = UserComponents['schemas']['LoginResponseContent'];
type RegisterInput = UserComponents['schemas']['RegisterRequestContent'];
type RegisterOutput = UserComponents['schemas']['RegisterResponseContent'];
type RefreshInput = UserComponents['schemas']['RefreshRequestContent'];
type RefreshOutput = UserComponents['schemas']['RefreshResponseContent'];

type CreateCircleInput = CirclesComponents['schemas']['CreateCircleRequestContent'];
type CreateCircleOutput = CirclesComponents['schemas']['CreateCircleResponseContent'];
type UpdateCircleInput = CirclesComponents['schemas']['UpdateCircleRequestContent'];
type GetCircleOutput = CirclesComponents['schemas']['GetCircleResponseContent'];
type ListCircleMembersOutput = CirclesComponents['schemas']['ListCircleMembersResponseContent'];
type UserForCircleOutput = CirclesComponents['schemas']['ListCirclesForUserResponseContent'];
type AddCircleMemberInput = CirclesComponents['schemas']['AddUserToCircleRequestContent'];

type CreateExpenseInput = ExpenseComponents['schemas']['CreateExpenseRequestContent'];
type CreateExpenseOutput = ExpenseComponents['schemas']['CreateExpenseResponseContent'];
type GetExpenseOutput = ExpenseComponents['schemas']['GetExpenseResponseContent'];
type UpdateExpenseInput = ExpenseComponents['schemas']['UpdateExpenseRequestContent'];

type CreateExpenseListInput = ExpenseListComponents['schemas']['CreateExpenseListRequestContent'];
type CreateExpenseListOutput = ExpenseListComponents['schemas']['CreateExpenseListResponseContent'];
type UpdateExpenseListInput = ExpenseListComponents['schemas']['UpdateExpenseListRequestContent'];
type GetExpenseListsOutput = ExpenseListComponents['schemas']['GetExpenseListsResponseContent'];
type GetExpenseListOutput = ExpenseListComponents['schemas']['GetExpenseListResponseContent'];
type GetSettledExpenseListsOutput =
	ExpenseListComponents['schemas']['GetSettledExpenseListsResponseContent'];
type SettleExpenseListInput = ExpenseListComponents['schemas']['SettleExpenseListRequestContent'];

type CircleId = { circleId: string };
type UserId = { userId: string };
type MemberId = { memberId: string };
type ExpenseId = { expenseId: string };
type ExpenseListId = { expenseListId: string };

export interface UserService {
	login: (credentials: LoginInput) => Promise<LoginOutput>;
	refresh: (refreshToken: RefreshInput) => Promise<RefreshOutput>;
	register: (credentials: RegisterInput) => Promise<RegisterOutput>;
}

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

export interface ExpenseService {
	createExpense: (
		expenseListId: ExpenseListId,
		expenseInput: CreateExpenseInput
	) => Promise<CreateExpenseOutput>;
	getExpense: (expenseId: ExpenseId) => Promise<GetExpenseOutput>;
	updateExpense: (
		circleId: CircleId,
		expenseId: ExpenseId,
		updateExpenseInput: UpdateExpenseInput
	) => Promise<void>;
	deleteExpense: (expenseId: ExpenseId) => Promise<void>;
}

export interface ExpenseListService {
	createExpenseList: (
		circleId: CircleId,
		expenseListInput: CreateExpenseListInput
	) => Promise<CreateExpenseListOutput>;
	getExpenseLists: (circleId: CircleId) => Promise<GetExpenseListsOutput>;
	getExpenseList: (expenseListId: ExpenseListId) => Promise<GetExpenseListOutput>;
	deleteExpenseList: (expenseListId: ExpenseListId) => Promise<void>;
	updateExpenseList: (
		expenseListId: ExpenseListId,
		updateExpenseListInput: UpdateExpenseListInput
	) => Promise<void>;
	settleExpenseList: (
		expenseListId: ExpenseListId,
		settleExpenseListInput: SettleExpenseListInput
	) => Promise<void>;
	getSettledExpenseList: (expenseListId: ExpenseListId) => Promise<GetSettledExpenseListsOutput>;
}

export class UserClient implements UserService {
	private client = createClient<UserPaths>({ baseUrl: '/' });

	login = async (credentials: LoginInput): Promise<LoginOutput> => {
		const { data, error } = await this.client.POST('/api/auth/login', { body: credentials });
		return throwIfError(data, error);
	};

	refresh = async (refreshToken: RefreshInput): Promise<RefreshOutput> => {
		const { data, error } = await this.client.POST('/api/auth/refresh', { body: refreshToken });
		return throwIfError(data, error);
	};

	register = async (credentials: RegisterInput): Promise<RegisterOutput> => {
		const { data, error } = await this.client.POST('/api/auth/register', { body: credentials });
		return throwIfError(data, error);
	};
}

export class CirclesClient implements CirclesService {
	private client = createClient<CirclesPaths>({ baseUrl: '/' });

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

export class ExpenseClient implements ExpenseService {
	private client = createClient<ExpensePaths>({ baseUrl: '/' });

	createExpense = async (
		expenseListId: ExpenseListId,
		expenseInput: CreateExpenseInput
	): Promise<CreateExpenseOutput> => {
		const { error, data } = await this.client.POST('/api/expense_lists/{expenseListId}/expenses', {
			params: { path: expenseListId },
			body: expenseInput
		});
		return throwIfError(data, error);
	};

	getExpense = async (expenseId: ExpenseId): Promise<GetExpenseOutput> => {
		const { error, data } = await this.client.GET('/api/expenses/{expenseId}', {
			params: { path: expenseId }
		});
		return throwIfError(data, error);
	};

	updateExpense = async (
		circleId: CircleId,
		expenseId: ExpenseId,
		updateExpenseInput: UpdateExpenseInput
	): Promise<void> => {
		const { error } = await this.client.PATCH('/api/expenses/{expenseId}', {
			params: { path: { ...circleId, ...expenseId } },
			body: updateExpenseInput
		});
		throwIfError(undefined, error);
	};

	deleteExpense = async (expenseId: ExpenseId): Promise<void> => {
		const { error } = await this.client.DELETE('/api/expenses/{expenseId}', {
			params: { path: expenseId }
		});
		throwIfError(undefined, error);
	};
}

export class ExpenseListClient implements ExpenseListService {
	private client = createClient<ExpenseListPaths>({ baseUrl: '/' });

	createExpenseList = async (
		circleId: CircleId,
		expenseListInput: CreateExpenseListInput
	): Promise<CreateExpenseListOutput> => {
		const { error, data } = await this.client.POST('/api/circles/{circleId}/expenseLists', {
			params: { path: circleId },
			body: expenseListInput
		});
		return throwIfError(data, error);
	};

	getExpenseLists = async (circleId: CircleId): Promise<GetExpenseListsOutput> => {
		const { error, data } = await this.client.GET('/api/circles/{circleId}/expenseLists', {
			params: { path: circleId }
		});
		return throwIfError(data, error);
	};

	getExpenseList = async (expenseListId: ExpenseListId): Promise<GetExpenseListOutput> => {
		const { error, data } = await this.client.GET('/api/expenseLists/{expenseListId}', {
			params: { path: expenseListId }
		});
		return throwIfError(data, error);
	};

	deleteExpenseList = async (expenseListId: ExpenseListId): Promise<void> => {
		const { error } = await this.client.DELETE('/api/expenseLists/{expenseListId}', {
			params: { path: expenseListId }
		});
		throwIfError(undefined, error);
	};

	updateExpenseList = async (
		expenseListId: ExpenseListId,
		updateExpenseListInput: UpdateExpenseListInput
	): Promise<void> => {
		const { error } = await this.client.PUT('/api/expenseLists/{expenseListId}', {
			params: { path: expenseListId },
			body: updateExpenseListInput
		});
		throwIfError(undefined, error);
	};

	settleExpenseList = async (
		expenseListId: ExpenseListId,
		settleExpenseListInput: SettleExpenseListInput
	): Promise<void> => {
		const { error, data } = await this.client.POST('/api/expenseLists/{expenseListId}/settle', {
			params: { path: expenseListId },
			body: settleExpenseListInput
		});
		return throwIfError(data, error);
	};

	getSettledExpenseList = async (
		expenseListId: ExpenseListId
	): Promise<GetSettledExpenseListsOutput> => {
		const { error, data } = await this.client.GET('/api/expenseLists/{expenseListId}/settle', {
			params: { path: expenseListId }
		});
		return throwIfError(data, error);
	};
}
