import type { Middleware } from 'openapi-fetch';
import { userClient } from '../UserService';
import { AuthenticationError } from './ErrorHandling';

class AuthMiddleware implements Middleware {
	private userClient = userClient;

	onRequest = async (req: Request) => {
		const accessToken = await this.findToken();
		req.headers.set('Authorization', `Bearer ${accessToken}`);
		return req;
	};

	private findToken = async (): Promise<string> => {
		const localStorageToken = localStorage.getItem('accessToken');
		const refreshToken = localStorage.getItem('refreshToken');

		if (localStorageToken) {
			return localStorageToken;
		} else if (refreshToken) {
			return (await this.userClient.refresh({ refreshToken })).accessToken;
		} else {
			throw new AuthenticationError('No token found, please log in again.');
		}
	};
}

export const authMiddleware = new AuthMiddleware();
