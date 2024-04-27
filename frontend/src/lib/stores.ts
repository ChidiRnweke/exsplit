import { writable } from 'svelte/store';
import { userClient } from './api/clients/UserService';
import { goto } from '$app/navigation';

export const loggedInStatusStore = writable(false);
export const userIdStore = writable<string | null>(null);

export const storeLoginData = (userId: string) => {
	loggedInStatusStore.set(true);
	userIdStore.set(userId);
};

export const logout = (redirect: () => void) => {
	loggedInStatusStore.set(false);
	userIdStore.set(null);
	localStorage.removeItem('accessToken');
	localStorage.removeItem('refreshToken');
	localStorage.removeItem('userId');
	redirect();
};

export const initialState = async () => {
	const maybeUser = localStorage.getItem('userId');
	const refreshToken = localStorage.getItem('refreshToken');

	if (maybeUser && refreshToken) {
		const { error } = await userClient.refresh({ refreshToken });
		const user = localStorage.getItem('userId');
		// The user client takes care of storing the new access token as well as the user id
		// so we only need to set the user id here
		userIdStore.set(user);
		if (error) {
			logout(() => goto('login'));
		} else {
			loggedInStatusStore.set(true);
		}
	}
};
