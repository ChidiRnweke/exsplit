import { writable } from 'svelte/store';

export const loggedInStatusStore = writable(false);
export const userIdStore = writable<string | null>(null);

export const storeLoginData = (userId: string) => {
	loggedInStatusStore.set(true);
	userIdStore.set(userId);
};

export const handleLogout = () => {
	loggedInStatusStore.set(false);
	userIdStore.set(null);
};
