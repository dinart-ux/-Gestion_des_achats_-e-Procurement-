
import { useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import { useToast } from '../components/notifications/ToastContext';
import workflowApi from './workflowApi';

export default function NotificationsSocket() {
    const { showToast } = useToast();

    useEffect(() => {
        const token = localStorage.getItem('token');

        if (!token) return;

        const notificationsTraitees = new Set();

        const traiterNotification = async (notification) => {
            if (!notification?.message) return;

            const id = notification.id;

            if (id != null && notificationsTraitees.has(id)) return;

            if (id != null) {
                notificationsTraitees.add(id);
            }

            showToast(
                notification.message,
                notification.type || 'success'
            );

            if (id != null) {
                try {
                    await workflowApi.patch(
                        `/notifications/${id}/lue`
                    );
                } catch (error) {
                    notificationsTraitees.delete(id);
                    console.error(
                        'Impossible de marquer la notification comme lue :',
                        error
                    );
                }
            }
        };

        // Écouter les événements métier.
        const traiterMiseAJour = (message) => {
            try {
                const evenement = JSON.parse(message.body);

                console.log(
                    'Événement métier reçu :',
                    evenement
                );

                // La synchronisation des pages sera ajoutée
                // progressivement dans les prochaines étapes.
                window.dispatchEvent(
                    new CustomEvent('workflow-update', {
                        detail: evenement,
                    })
                );

            } catch (error) {
                console.error(
                    'Erreur de lecture de l’événement métier :',
                    error
                );
            }
        };

        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws',

            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },

            reconnectDelay: 5000,

            onConnect: async () => {
                console.log('WebSocket connecté.');

                // Notifications personnelles.
                client.subscribe(
                    '/user/queue/notifications',
                    (message) => {
                        try {
                            const notification = JSON.parse(message.body);
                            traiterNotification(notification);
                        } catch (error) {
                            console.error(
                                'Erreur de lecture de la notification :',
                                error
                            );
                        }
                    }
                );

                // Événements de mise à jour métier.
                client.subscribe(
                    '/user/queue/updates',
                    traiterMiseAJour
                );

                // Récupérer les notifications non lues.
                try {
                    const response = await workflowApi.get(
                        '/notifications/non-lues'
                    );

                    for (const notification of response.data) {
                        await traiterNotification(notification);
                    }

                    console.log(
                        'Notifications non lues récupérées :',
                        response.data.length
                    );
                } catch (error) {
                    console.error(
                        'Erreur de récupération des notifications :',
                        error
                    );
                }
            },

            onStompError: (frame) => {
                console.error(
                    'Erreur STOMP :',
                    frame.headers['message']
                );
            },

            onWebSocketError: (error) => {
                console.error('Erreur WebSocket :', error);
            },
        });

        client.activate();

        return () => {
            client.deactivate();
        };
    }, [showToast]);

    return null;
}
