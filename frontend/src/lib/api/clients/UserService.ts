import createClient from 'openapi-fetch';
import { throwIfError } from './util/ErrorHandling';
import type { paths } from '../schemas/exsplit.spec.UserService';
import type {
	LoginInput,
	LoginOutput,
	RefreshInput,
	RefreshOutput,
	RegisterInput,
	RegisterOutput
} from '../types/user';

export interface UserService {
	login: (credentials: LoginInput) => Promise<LoginOutput>;
	refresh: (refreshToken: RefreshInput) => Promise<RefreshOutput>;
	register: (credentials: RegisterInput) => Promise<RegisterOutput>;
}
class UserClient implements UserService {
	private client = createClient<paths>({ baseUrl: '/' });

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

export const userClient = new UserClient();
