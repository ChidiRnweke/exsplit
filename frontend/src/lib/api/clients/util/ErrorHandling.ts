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

export class AuthenticationError extends Error {
	constructor(message: string) {
		super(message);
		this.name = 'AuthenticationError';
	}
}
